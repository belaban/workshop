package org.misc;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.MessageBatch;

/**
 * @author Bela Ban
 * @since x.y
 */
public class SampleReceiver extends ReceiverAdapter {
    private final String name;

    public SampleReceiver(String name) {
        this.name = name;
    }

    public SampleReceiver() {
        this.name = "";
    }

    @Override public void receive(Message msg) {
        System.out.printf("%s: -- received from %s: %s\n", this.name, msg.src(), msg.getObject());
    }

    @Override public void receive(MessageBatch batch) {
        System.out.printf("%s: -- message batch received from %s:\n", this.name, batch.sender());
        int cnt=1;
        for(Message msg: batch)
            System.out.printf("%d: %s\n", cnt++, msg.getObject());
    }

    public void viewAccepted(View view) {
        System.out.println(this.name + " -- view = " + view);
    }
}
