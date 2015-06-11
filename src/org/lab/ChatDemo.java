package org.lab;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatDemo extends ReceiverAdapter {
    protected JChannel            channel;
    protected static final String rsp="rsp from ";
    protected boolean             send_replies=false;

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    public void receive(Message msg) {
        String payload=(String)msg.getObject();
        boolean is_rsp=payload.startsWith(rsp);
        String line=(!is_rsp? "[" + msg.getSrc() + "]: " : "") + payload;
        System.out.println(line);
        if(send_replies && !payload.startsWith(rsp)) {
            Message reply=new Message(msg.src(), rsp + channel.getAddress()).setFlag(Message.Flag.OOB);
            try {
                channel.send(reply);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void start(String props, String name, boolean send_replies) throws Exception {
        this.send_replies=send_replies;
        channel=new JChannel(props);
        if(name != null)
            channel.name(name);
        channel.setReceiver(this);
        channel.connect("ChatCluster");
        eventLoop();
        channel.close();
    }

    private void eventLoop() {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                if(line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }
                Message msg=new Message(null, line);
                channel.send(msg);
            }
            catch(Exception e) {
            }
        }
    }


    public static void main(String[] args) throws Exception {
        String  props="config.xml";
        String  name=null;
        boolean send_replies=true;

        for(int i=0; i < args.length; i++) {
            if(args[i].equals("-props")) {
                props=args[++i];
                continue;
            }
            if(args[i].equals("-name")) {
                name=args[++i];
                continue;
            }
            if(args[i].equals("-send_replies")) {
                send_replies=true;
                continue;
            }
            help();
            return;
        }

        new ChatDemo().start(props, name, send_replies);
    }

    protected static void help() {
        System.out.println("ChatDemo [-props XML config] [-name name] [-send_replies]");
    }
}