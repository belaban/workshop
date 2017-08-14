package org.lab;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.util.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatDemo extends ReceiverAdapter {
    protected JChannel            channel;

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    public void receive(Message msg) {
        String payload=msg.getObject();
        System.out.printf("[%s]: %s\n", msg.getSrc(), payload);
    }


    private void start(String props, String name) throws Exception {
        channel=new JChannel(props).name(name).receiver(this);
        channel.connect("ChatCluster");
        JmxConfigurator.registerChannel(channel,Util.getMBeanServer(),"chat-channel",channel.getClusterName(),true);
        eventLoop();
        channel.close();
    }

    private void eventLoop() {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                if(line.startsWith("quit") || line.startsWith("exit"))
                    break;
                Message msg=new Message(null, line);
                channel.send(msg);
            }
            catch(Exception e) {
            }
        }
    }


    public static void main(String[] args) throws Exception {
        String  props="config.xml";
        String  name=null;

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

        new ChatDemo().start(props, name);
    }

    protected static void help() {
        System.out.println("ChatDemo [-props XML config] [-name name]");
    }
}