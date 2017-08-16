package org.lab;

import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.blocks.*;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.stack.DiagnosticsHandler;
import org.jgroups.util.Average;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * RPC demo with slow receivers. Shows how thread pool configuration and AIA can help prevent pool exhaustion
 * @author Bela Ban
 */
public class Advanced implements MembershipListener {
    protected JChannel           ch;
    protected RpcDispatcher      disp;
    protected int                num_threads=5;
    protected boolean            oob;
    protected long               sleep=1000; // ms
    protected Invoker[]          invokers;
    protected Average            avg; // avg invocation times
    protected volatile boolean   running;
    protected ThreadPoolExecutor app_thread_pool=(ThreadPoolExecutor)Executors.newCachedThreadPool();

    protected static final Method SLEEP;

    static {
        try {
            SLEEP=Advanced.class.getMethod("sleep");
        }
        catch(NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    @ManagedAttribute(description="avg invocation time")
    public double getAvgInvocationTime() {
        return avg != null? avg.getAverage() : 0;
    }

    @ManagedAttribute(description="Size of the app thread pool")
    public int getAppPoolSize() {return app_thread_pool.getPoolSize();}

    @ManagedAttribute(description="Number of active threads in the app thread pool")
    public int getAppPoolActiveThreads() {return app_thread_pool.getActiveCount();}


    protected void start(String props, String name, boolean use_async_request_handler) throws Exception {
        ch=new JChannel(props).name(name);
        disp=new RpcDispatcher(ch, this).setMembershipListener(this);
        if(use_async_request_handler) {
            disp.asyncDispatching(true);
            disp.setRequestHandler(new MyAsyncHandler(disp));
        }
        ch.connect("advanced");
        Util.registerChannel(ch, "advanced-cluster");
        JmxConfigurator.register(this, Util.getMBeanServer(), "advanced:obj=advanced-obj");

        ch.getProtocolStack().getTransport().registerProbeHandler(new DiagnosticsHandler.ProbeHandler() {
            public Map<String,String> handleProbe(String... keys) {
                Map<String,String> map=new HashMap<>();
                for(String key: keys) {
                    if(key.equals("adv")) {
                        map.put("adv.invocation_avg", String.valueOf(getAvgInvocationTime()));
                        map.put("adv.app_thread_pool_size", String.valueOf(getAppPoolSize()));
                        map.put("adv.app_thread_pool_active_count", String.valueOf(getAppPoolActiveThreads()));
                        map.put("adv.app_thread_pool_largest_count", String.valueOf(app_thread_pool.getLargestPoolSize()));
                    }
                }
                return map;
            }

            public String[] supportedKeys() {
                return new String[]{"adv"};
            }
        });

        eventLoop();
        Util.close(ch);
    }

    protected synchronized void startInvokers() {
        if(invokers != null)
            return;
        invokers=new Invoker[num_threads];
        avg=new Average();
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
                    app_thread_pool.shutdown();
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
        boolean use_async_handler=false;

        for(int i=0; i < args.length; i++) {
            if(args[i].equals("-props")) {
                props=args[++i];
                continue;
            }
            if(args[i].equals("-name")) {
                name=args[++i];
                continue;
            }
            if(args[i].equals("-use_async_handler")) {
                use_async_handler=true;
                continue;
            }
            System.out.println("Advanced [-props props] [-name name] [-use_async_handler]");
            return;
        }
        Advanced ad=new Advanced();
        ad.start(props, name, use_async_handler);
    }


    protected class Invoker extends Thread {
        MethodCall call=new MethodCall(SLEEP);

        public void run() {
            while(running) {
                try {
                    RequestOptions opts=RequestOptions.SYNC();
                    if(oob)
                        opts.flags(Message.Flag.OOB);
                    long start=System.currentTimeMillis();
                    disp.callRemoteMethods(null, call, opts);
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

        public void runInBackground() {
            while(running) {
                try {
                    RequestOptions opts=RequestOptions.SYNC();
                    if(oob)
                        opts.flags(Message.Flag.OOB);
                    long start=System.currentTimeMillis();
                    CompletableFuture<RspList<Void>> fut=disp.callRemoteMethodsWithFuture(null, call, opts);
                    fut.whenComplete((rsps,ex) -> {
                        long diff=System.currentTimeMillis() - start;
                        if(diff > 0) {
                            avg.add(diff);
                            System.out.println(diff);
                        }
                    });
                    Util.sleep(500);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    protected class MyAsyncHandler implements RequestHandler {
        protected final RpcDispatcher d;

        public MyAsyncHandler(RpcDispatcher d) {
            this.d=d;
        }

        @Override
        public void handle(final Message request, final Response response) throws Exception {
            app_thread_pool.execute(() -> {
                try {
                    Object result=d.handle(request);
                    response.send(result, false);
                }
                catch(Exception e) {
                    response.send(e, true);
                }
            });
        }

        @Override
        public Object handle(Message msg) throws Exception {
            return d.handle(msg);
        }
    }
}
