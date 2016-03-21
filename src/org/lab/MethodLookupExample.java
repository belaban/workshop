package org.lab;

import org.jgroups.JChannel;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Bela Ban
 * @since x.y
 */
public class MethodLookupExample {
    protected JChannel      ch;
    protected RpcDispatcher disp;

    protected static final Method MULT, SUB;
    protected static final Map<Short,Method> lookup=new HashMap<>();

    static {
        try {
            MULT=MethodLookupExample.class.getMethod("mult", int.class);
            SUB=MethodLookupExample.class.getMethod("sub", int.class, int.class);
            lookup.put((short)1,MULT);
            lookup.put((short)2, SUB);
        }
        catch(NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static int mult(int a) {return a*a;}
    public static int sub(int a,int b) {return a-b;}




    protected void start() throws Exception {
        ch=new JChannel("config.xml").name("A");
        disp=new RpcDispatcher(ch, this);
        disp.setMethodLookup(lookup::get);
        ch.connect("demo");

        for(int i=1; i <= 10; i++) {

            MethodCall call=new MethodCall((short)1,i);
            RspList<Integer> rsps=disp.callRemoteMethods(null,call,
                                                         RequestOptions.SYNC().setTimeout(60000));
            System.out.println("rsps = " + rsps);
            Util.sleep(1000);
        }
        Util.close(disp,ch);
    }



    public static void main(String[] args) throws Exception {
        new MethodLookupExample().start();
    }
}
