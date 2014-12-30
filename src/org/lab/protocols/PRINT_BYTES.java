package org.lab.protocols;

import org.jgroups.Event;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.util.MessageBatch;

/**
 * @author Bela Ban
 * @since x.y
 */
public class PRINT_BYTES extends Protocol {
    protected static final short ID=2015;

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
        return super.down(evt);
    }

    public Object up(Event evt) {
        return super.up(evt);
    }

    public void up(MessageBatch batch) {
        super.up(batch);
    }
}
