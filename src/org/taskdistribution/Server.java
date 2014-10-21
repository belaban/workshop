package org.taskdistribution;

import org.jgroups.*;
import org.jgroups.util.Promise;
import org.jgroups.util.Streamable;
import org.jgroups.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Bela Ban
 */
public class Server extends ReceiverAdapter implements Master, Slave {
    private String  props="udp.xml";
    private Channel ch;

    /** Maps task IDs to Tasks */
    private final ConcurrentMap<ClusterID,Entry> tasks=new ConcurrentHashMap<ClusterID,Entry>();

    /** Used to handle received tasks */
    private final ExecutorService thread_pool=Executors.newCachedThreadPool();

    private View view;
    private int  rank=-1;
    private int  cluster_size=-1;


    public Server(String props) {
        this.props=props;
    }

    public void start(String name) throws Exception {
        ch=new JChannel(props).name(name);
        ch.setReceiver(this);
        ch.connect("dzone-demo");
    }

    public void stop() {
        thread_pool.shutdown();
        ch.close();
    }

    public String info() {
        StringBuilder sb=new StringBuilder();
        sb.append("local_addr=" + ch.getAddress() + "\nview=" + view).append("\n");
        sb.append("rank=" + rank + "\n");
        sb.append("(" + tasks.size() + " entries in tasks cache)");
        return sb.toString();
    }

    public Object submit(Task task, long timeout) throws Exception {
        ClusterID id=ClusterID.create(ch.getAddress());
        try {
            Request req=new Request(Request.Type.EXECUTE, task, id, null);
            byte[] buf=Util.streamableToByteBuffer(req);
            Entry entry=new Entry(task, ch.getAddress());
            tasks.put(id, entry);
            log("==> submitting " + id);
            ch.send(new Message(null, buf));
            // wait on entry for result
            return entry.promise.getResultWithTimeout(timeout);
        }
        catch(Exception ex) {
            tasks.remove(id); // remove it again
            throw ex;
        }
    }

    public Object handle(Task task) {
        return task.execute();
    }

    /** All we receive is Requests */
    public void receive(Message msg) {
        try {
            Request req=(Request)Util.streamableFromByteBuffer(Request.class, msg.getRawBuffer(), msg.getOffset(), msg.getLength());
            switch(req.type) {
                case EXECUTE:
                    handleExecute(req.id, msg.getSrc(), req.task);
                    break;
                case RESULT:
                    Entry entry=tasks.get(req.id);
                    if(entry == null) {
                        err("found no entry for request " + req.id);
                    }
                    else {
                        entry.promise.setResult(req.result);
                    }
                    multicastRemoveRequest(req.id);
                    break;
                case REMOVE:
                    tasks.remove(req.id);
                    break;
                default:
                    throw new IllegalArgumentException("type " + req.type + " is not recognized");
            }
        }
        catch(Exception e) {
            err("exception receiving message from " + msg.getSrc(), e);
        }
    }

    private void multicastRemoveRequest(ClusterID id) {
        Request remove_req=new Request(Request.Type.REMOVE, null, id, null);
        try {
            byte[] buf=Util.streamableToByteBuffer(remove_req);
            ch.send(new Message(null, buf));
        }
        catch(Exception e) {
            err("failed multicasting REMOVE request", e);
        }
    }

    private void handleExecute(ClusterID id, Address sender, Task task) {
        tasks.putIfAbsent(id, new Entry(task, sender));
        int index=id.getId() % cluster_size;
        if(index != rank) {
            return;
        }

        // System.out.println("ID=" + id + ", index=" + index + ", my rank=" + rank);
        execute(id, sender, task);
    }

    private void execute(ClusterID id, Address sender, Task task) {
        Handler handler=new Handler(id, sender, task);
        thread_pool.execute(handler);
    }


    public void viewAccepted(View view) {
        List<Address> left_members=this.view != null && view != null?
          Util.leftMembers(this.view.getMembers(), view.getMembers()) : null;
        this.view=view;
        Address local_addr=ch.getAddress();
        System.out.println("view: " + view);
        cluster_size=view.size();
        List<Address> mbrs=view.getMembers();
        int old_rank=rank;
        for(int i=0; i < mbrs.size(); i++) {
            Address tmp=mbrs.get(i);
            if(tmp.equals(local_addr)) {
                rank=i;
                break;
            }
        }
        if(old_rank == -1 || old_rank != rank)
            log("my rank is " + rank);

        // process tasks by left members
        if(left_members != null && !left_members.isEmpty()) {
            for(Address mbr: left_members) {
                handleLeftMember(mbr);
            }
        }
    }

    /** Take over the tasks previously assigned to this member *if* the ID matches my (new rank) */
    private void handleLeftMember(Address mbr) {
        for(Map.Entry<ClusterID,Entry> entry: tasks.entrySet()) {
            ClusterID id=entry.getKey();
            int index=id.getId() % cluster_size;
            if(index != rank)
                return;
            Entry val=entry.getValue();
            if(mbr.equals(val.submitter)) {
                err("will not take over tasks submitted by " + mbr + " because it left the cluster");
                continue;
            }
            log("**** taking over task " + id + " from " + mbr + " (submitted by " + val.submitter + ")");
            execute(id, val.submitter, val.task);
        }
    }

    public static void main(String[] args) throws Exception {
        String props="udp.xml";
        String name=null;

        for(int i=0; i < args.length; i++) {
            if(args[i].equals("-props")) {
                props=args[++i];
                continue;
            }
            if(args[i].equals("-name")) {
                name=args[++i];
                continue;
            }
            help();
            return;
        }
        Server server=new Server(props);
        server.start(name);

        loop(server);
    }

    private static void loop(Server server) {
        boolean looping=true;
        while(looping) {
            int key=Util.keyPress("[1] Submit [2] Submit long running task [3] Info [q] Quit");
            switch(key) {
                case '1':
                    Task task=new Task() {
                        private static final long serialVersionUID=5102426397394071700L;

                        public Object execute() {
                            return new Date();
                        }
                    };
                    _submit(task, server);
                    break;
                case '2':
                    task=new Task() {
                        private static final long serialVersionUID=5102426397394071700L;

                        public Object execute() {
                            System.out.println("sleeping for 15 secs...");
                            Util.sleep(15000);
                            System.out.println("done");
                            return new Date();
                        }
                    };
                    _submit(task, server);
                    break;
                case '3':
                    System.out.println(server.info());
                    break;
                case 'q':
                    looping=false;
                    break;
                case 'r':
                    break;
                case '\n':
                    break;
                case -1:
                    looping=false;
                    break;
            }
        }
        server.stop();
    }

    private static void _submit(Task task, Server server) {
        try {
            Object result=server.submit(task, 30000);
            log("<== result = " + result);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    private static void help() {
        System.out.println("Server [-props <XML config file>] [-name <name>]");
    }

    private static void log(String msg) {
        System.out.println(msg);
    }


    private static void err(String msg) {
        System.err.println(msg);
    }

    private static void err(String msg, Throwable t) {
        System.err.println(msg + ", ex=" + t);
    }

    private static class Entry {
        private final Task            task;
        private final Address         submitter;
        private final Promise<Object> promise=new Promise<Object>();

        public Entry(Task task, Address submitter) {
            this.task=task;
            this.submitter=submitter;
        }
    }

    private class Handler implements Runnable {
        final ClusterID id;
        final Address sender;
        final Task task;

        public Handler(ClusterID id, Address sender, Task task) {
            this.id=id;
            this.sender=sender;
            this.task=task;
        }

        public void run() {
            Object result=null;
            if(task != null) {
                try {
                    log("executing " + id);
                    result=handle(task);
                }
                catch(Throwable t) {
                    err("failed executing " + id, t);
                    result=t;
                }
            }
            Request response=new Request(Request.Type.RESULT, null, id, result);
            try {
                byte[] buf=Util.streamableToByteBuffer(response);
                Message rsp=new Message(sender, buf);
                ch.send(rsp);
            }
            catch(Exception e) {
                err("failed executing task " + id, e);
            }
        }
    }
    

    public static class Request implements Streamable {
        static enum Type {EXECUTE, RESULT, REMOVE};

        private Type type;
        private Task task;
        private ClusterID id;
        private Object result;

        
        public Request() {
        }

        public Request(Type type, Task task, ClusterID id, Object result) {
            this.type=type;
            this.task=task;
            this.id=id;
            this.result=result;
        }

       
        public void writeTo(DataOutput out) throws Exception {
            out.writeInt(type.ordinal());
            try {
                Util.objectToStream(task, out);
            }
            catch(Exception e) {
                IOException ex=new IOException("failed marshalling of task " + task);
                ex.initCause(e);
                throw ex;
            }
            Util.writeStreamable(id, out);
            try {
                Util.objectToStream(result, out);
            }
            catch(Exception e) {
                IOException ex=new IOException("failed to marshall result object");
                ex.initCause(e);
                throw ex;
            }
        }

        public void readFrom(DataInput in) throws Exception {
            int tmp=in.readInt();
            switch(tmp) {
                case 0:
                    type=Type.EXECUTE;
                    break;
                case 1:
                    type=Type.RESULT;
                    break;
                case 2:
                    type=Type.REMOVE;
                    break;
                default:
                    throw new InstantiationException("ordinal " + tmp + " cannot be mapped to enum");
            }
            try {
                task=(Task)Util.objectFromStream(in);
            }
            catch(Exception e) {
                InstantiationException ex=new InstantiationException("failed reading task from stream");
                ex.initCause(e);
                throw ex;
            }
            id=(ClusterID)Util.readStreamable(ClusterID.class, in);
            try {
                result=Util.objectFromStream(in);
            }
            catch(Exception e) {
                IOException ex=new IOException("failed to unmarshal result object");
                ex.initCause(e);
                throw ex;
            }
        }

    }
    
}
