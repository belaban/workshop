package org.misc;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

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

    public void receive(Message msg) {
        System.out.println(this.name + " -- received from " + msg.src() + ": " + msg.getObject());
    }

    public void viewAccepted(View view) {
        System.out.println(this.name + " -- view = " + view);
    }
}
