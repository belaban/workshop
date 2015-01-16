package org.lab;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.util.Average;
import org.jgroups.util.Util;

/**
 * RPC demo with slow receivers. Shows how thread pool configuration and AIA can help prevent pool exhaustion
 * @author Bela Ban
 */
public class Advanced extends ReceiverAdapter {
    protected JChannel          ch;
    protected RpcDispatcher     disp;
    protected int               num_threads=5;
    protected boolean           oob=false;
    protected long              sleep=1000; // ms
    protected Invoker[]         invokers;
    protected Average           avg; // avg invocation times
    protected volatile boolean  running=false;


    @ManagedAttribute(description="avg invocation time")
    public double getAvgInvocationTime() {
        return avg != null? avg.getAverage() : 0;
    }

    protected void start(String props, String name) throws Exception {
        ch=new JChannel(props);
        if(name != null)
            ch.name(name);
        disp=new RpcDispatcher(ch, null, this, this);
        ch.connect("advanced");
        Util.registerChannel(ch, "advanced-cluster");
        JmxConfigurator.register(this, Util.getMBeanServer(), "advanced:obj=advanced-obj");
        eventLoop();
        Util.close(ch);
    }

    protected synchronized void startInvokers() {
        if(invokers != null)
            return;
        invokers=new Invoker[num_threads];
        avg=new Average(num_threads);
        running=true;
        for(int i=0; i < invokers.length; i++) {
            invokers[i]=new Invoker();
            invokers[i].start();
        }
    }

    protected synchronized void stopInvokers() {
        running=false;
        invokers=null;
    }

    // callback
    public void sleep() {
        Util.sleep(sleep);
    }

    @Override
    public void viewAccepted(View view) {
        System.out.println("-- view: " + view);
    }

    protected void eventLoop() {
        while(true) {
            int key=Util.keyPress("[1] start (running=" + running + ") [2] stop [3] num_threads (" + num_threads + ") [o] oob (" + oob + ") " +
                                    "[5] info [6] sleep (" + sleep + ") [x] exit");
            switch(key) {
                case '1':
                    startInvokers();
                    break;
                case '2':
                    stopInvokers();
                    break;
                case '3':
                    try {
                        num_threads=Util.readIntFromStdin("num_threads: ");
                    } catch(Exception e) {}
                    break;
                case 'o':
                    oob=!oob;
                    break;
                case '5':
                    dumpInfo();
                    break;
                case '6':
                    try {
                        sleep=(long)Util.readIntFromStdin("sleep (ms): ");
                    } catch(Exception e) {}
                    break;
                case 'x':
                    return;
            }
        }
    }

    protected void dumpInfo() {
        System.out.println("avg invocation time: " + avg);
    }

    public static void main(String[] args) throws Exception {
        String props="config.xml";
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
            System.out.println("Advanced [-props props] [-name name]");
            return;
        }
        Advanced ad=new Advanced();
        ad.start(props, name);
    }


    protected class Invoker extends Thread {

        public void run() {
            while(running) {
                try {
                    RequestOptions opts=RequestOptions.SYNC();
                    if(oob)
                        opts.setFlags(Message.Flag.OOB);
                    long start=System.currentTimeMillis();
                    disp.callRemoteMethods(null, "sleep", null, null, opts);
                    long diff=System.currentTimeMillis() - start;
                    if(diff > 0) {
                        avg.add(diff);
                        System.out.println(diff);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
