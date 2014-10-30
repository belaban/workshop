package org.misc;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

/**
 * @author Bela Ban
 * @since x.y
 */
public class SampleReceiver extends ReceiverAdapter {

    public void receive(Message msg) {
        System.out.println("received from " + msg.src() + ": " + msg.getObject());
    }

    public void viewAccepted(View view) {
        System.out.println("view = " + view);
    }


}
