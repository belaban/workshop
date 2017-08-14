package org.lab.protocols;

import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.ManagedOperation;
import org.jgroups.annotations.Property;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.util.MessageBatch;
import org.jgroups.util.Util;


@MBean(description="Demo protocol")
public class PRINT_BYTES extends Protocol {

    @Property(description="Suppresses printing to stdout if false")
    protected boolean do_print=true;

    protected boolean is_registered;

    @ManagedOperation(description="Register to jmx")
    public synchronized void register(boolean register) {
        try {
            if(register && !is_registered) {
                JmxConfigurator.registerChannel(this.getProtocolStack().getChannel(), Util.getMBeanServer(), this.getTransport().getClusterName());
                is_registered=true;
            }
            else {
                if(is_registered) {
                    JmxConfigurator.unregisterChannel(this.getProtocolStack().getChannel(), Util.getMBeanServer(), this.getTransport().getClusterName());
                    is_registered=false;
                }
            }
        }
        catch(Exception e) {
            log.warn(e.getMessage(), e);
        }
    }


    public void init() throws Exception {
        super.init();
    }

    public void start() throws Exception {
        super.start();
    }

    public void stop() {
        super.stop();
    }

    public void destroy() {
        super.destroy();
    }

    public Object down(Event evt) {
        switch(evt.getType()) {
            case Event.VIEW_CHANGE:
                View view=evt.getArg();
                System.out.println("view = " + view);
                break;
        }
        return down_prot.down(evt);
    }

    public Object down(Message msg) {
        if(do_print) {
            int num_bytes=msg.getLength();
            if(num_bytes > 0)
                System.out.printf("-- sending %d bytes\n", num_bytes);
            System.out.println("headers are " + msg.printHeaders());
        }
        return down_prot.down(msg);
    }

    public Object up(Message msg) {
        if(do_print) {
            int num_bytes=msg.getLength();
            if(num_bytes > 0)
                System.out.printf("-- received %d bytes from %s\n", num_bytes, msg.src());
        }
        return up_prot.up(msg);
    }

    public void up(MessageBatch batch) {
        int total_bytes=0;
        for(Message msg: batch)
            total_bytes+=msg.getLength();
        // alternative: total_bytes=batch.length();
        System.out.printf("received batch of %d messages from %s: total size is %d bytes\n",
                          batch.size(), batch.sender(), total_bytes);
        if(!batch.isEmpty())
            up_prot.up(batch);
    }
}
