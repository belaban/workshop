package org.lab.protocols;

import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.util.MessageBatch;


/**
 * @author Bela Ban
 * @since x.y
 */
public class PRINT_BYTES extends Protocol {
    protected static final short ID=2015;

    @Property(description="Suppresses printing to stdout if false")
    protected boolean do_print=true;

    static {
        ClassConfigurator.addProtocol(ID, PRINT_BYTES.class);
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
                View view=(View)evt.getArg();
                System.out.println("view = " + view);
                break;
            case Event.MSG:
                if(do_print) {
                    Message msg=(Message)evt.getArg();
                    int num_bytes=msg.getLength();
                    if(num_bytes > 0)
                        System.out.printf("-- sending %d bytes\n", num_bytes);
                    // System.out.println("headers are " + msg.getHeaders());
                }
                break;
        }
        return down_prot.down(evt);
    }

    public Object up(Event evt) {
        switch(evt.getType()) {
            case Event.MSG:
                if(do_print) {
                    Message msg=(Message)evt.getArg();
                    int num_bytes=msg.getLength();
                    if(num_bytes > 0)
                        System.out.printf("-- received %d bytes\n", num_bytes);
                }
                break;
        }
        return up_prot.up(evt);
    }

    public void up(MessageBatch batch) {
        int total_bytes=0;
        for(Message msg: batch)
            total_bytes+=msg.getLength();
        // alternative: total_bytes=batch.length();
        System.out.printf("received batch of %d messages: total size is %d bytes\n", batch.size(), total_bytes);
        if(!batch.isEmpty())
            up_prot.up(batch);
    }
}
