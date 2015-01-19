package org.lab;

import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatDemoRpc extends ReceiverAdapter {
    protected JChannel      channel;
    protected RpcDispatcher disp;

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    public void onMessage(String message) {
        System.out.println(message);
    }


    private void start(String props, String name) throws Exception {
        channel=new JChannel(props);
        if(name != null)
            channel.name(name);
        disp=new RpcDispatcher(channel, null, this, this);
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
                String message=channel.getAddressAsString() + ": " + line;
                RspList<Void> rsps=disp.callRemoteMethods(null, "onMessage", new Object[]{message}, new Class[]{String.class}, RequestOptions.SYNC());
                printResponses(rsps);
            }
            catch(Exception e) {
            }
        }
    }

    protected static void printResponses(RspList<Void> rsps) {
        boolean first=true;
        System.out.print("responses from [");
        for(Rsp rsp: rsps) {
            if(first)
                first=false;
            else
                System.out.print(", ");
            System.out.print(rsp.getSender());
        }
        System.out.println("]");
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

        new ChatDemoRpc().start(props, name);
    }

    protected static void help() {
        System.out.println("ChatDemo [-props XML config] [-name name]");
    }
}