package org.lab;

import org.jgroups.*;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.Util;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SimpleFileTransfer extends ReceiverAdapter {
    protected String   filename;
    protected JChannel channel;
    protected Map<String,OutputStream> files=new ConcurrentHashMap<>();
    protected static final short ID=3500;

    private void start(String name, String filename) throws Exception {
        ClassConfigurator.add((short)3500, FileHeader.class);
        this.filename=filename;
        channel=new JChannel("/home/bela/fast.xml").name(name);
        channel.setReceiver(this);
        channel.connect("FileCluster");
        eventLoop();
    }

    private void eventLoop() throws Exception {
        while(true) {
            Util.keyPress(String.format("<enter to send %s>\n", filename));
            sendFile();
        }
    }

    protected void sendFile() throws Exception {
        FileInputStream in=new FileInputStream(filename);

        try {
            for(;;) {
                byte[] buf=new byte[8096];
                int bytes=in.read(buf);
                if(bytes == -1)
                    break;
                // System.out.printf("sending {%d %d %d}\n", buf[0], buf[1], buf[2]);
                sendMessage(buf, 0, bytes, false);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            sendMessage(null, 0, 0, true);
        }
    }


    public void receive(Message msg) {
        // System.out.printf("received %s from %s\n", Util.printBytes(msg.getLength()), msg.src());

        byte[] buf=msg.getRawBuffer();

        if(buf != null)
            System.out.printf("received {%d %d %d}\n", buf[0], buf[1], buf[2]);

        FileHeader hdr=(FileHeader)msg.getHeader(ID);
        if(hdr == null)
            return;
        OutputStream out=files.get(hdr.filename);
        try {
            if(out == null) {
                File tmp=new File(hdr.filename);
                String fname=tmp.getName();
                fname="/tmp/" + fname;
                out=new FileOutputStream(fname);
                System.out.printf("-- creating file %s\n", hdr.filename);
                files.put(hdr.filename, out);
            }
            if(hdr.eof) {
                System.out.printf("closing %s\n", hdr.filename);
                Util.close(files.remove(hdr.filename));
            }
            else {
                System.out.printf("appending %d bytes\n", msg.getLength());
                out.write(msg.getRawBuffer(), msg.getOffset(), msg.getLength());
            }
        }
        catch(Throwable t) {
            System.err.println(t);
        }
    }


    protected void sendMessage(byte[] buf, int offset, int length, boolean eof) throws Exception {
        Message msg=new Message(null, buf, offset, length).putHeader(ID, new FileHeader(filename, eof))
          .setFlag(Message.Flag.DONT_BUNDLE);
        // set this if the sender doesn't want to receive the file
        // msg.setTransientFlag(Message.TransientFlag.DONT_LOOPBACK);
        channel.send(msg);
    }


    /*protected static Buffer readFile(String filename) throws Exception {
        File file=new File(filename);
        int size=(int)file.length();
        FileInputStream input=new FileInputStream(file);
        ByteArrayDataOutputStream out=new ByteArrayDataOutputStream(size);
        byte[] read_buf=new byte[1024];
        int bytes;
        while((bytes=input.read(read_buf)) != -1)
            out.write(read_buf, 0, bytes);
        return out.getBuffer();
    }*/


    protected static class FileHeader extends Header {
        protected String  filename;
        protected boolean eof;

        public FileHeader() {} // for de-serialization

        public FileHeader(String filename, boolean eof) {
            this.filename=filename;
            this.eof=eof;
        }

        public int size() {
            return Util.size(filename) + Global.BYTE_SIZE;
        }

        public void writeTo(DataOutput out) throws Exception {
            Util.writeObject(filename, out);
            out.writeBoolean(eof);
        }

        public void readFrom(DataInput in) throws Exception {
            filename=(String)Util.readObject(in);
            eof=in.readBoolean();
        }
    }



    public static void main(String[] args) throws Exception {
        if(args.length != 2) {
            System.out.printf("%s <name> <filename>\n", SimpleFileTransfer.class.getSimpleName());
            return;
        }
        new SimpleFileTransfer().start(args[0], args[1]); // name and file
    }
}
