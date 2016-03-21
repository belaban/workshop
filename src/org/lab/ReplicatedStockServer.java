package org.lab;

import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Replicated stock server; every cluster node has the same state (stocks).
 */
public class ReplicatedStockServer extends ReceiverAdapter {
    private final Map<String,Double> stocks=new HashMap<>();
    private JChannel                 channel;
    private RpcDispatcher            disp; // to invoke RPCs


    /** Assigns a value to a stock */
    public void _setStock(String name, double value) {
        synchronized(stocks) {
            stocks.put(name,value);
            System.out.printf("-- set %s to %s\n",name,value);
        }
    }

    /** Removes a stock from the hashmap */
    public void _removeStock(String name) {
        synchronized(stocks) {
            stocks.remove(name);
            System.out.printf("-- removed %s\n",name);
        }
    }

    private void start(String props) throws Exception {
        channel=new JChannel(props);
        disp=new RpcDispatcher(channel, this).setMembershipListener(this);
        disp.setStateListener(this);
        channel.connect("stocks");
        disp.start();
        channel.getState(null, 30000); // fetches the state from the coordinator
        while(true) {
            int c=Util.keyPress("[1] Show stocks [2] Get quote [3] Set quote [4] Remove quote [x] Exit");
            try {
                switch(c) {
                    case '1':
                        showStocks();
                        break;
                    case '2':
                        getStock();
                        break;
                    case '3':
                        setStock();
                        break;
                    case '4':
                        removeStock();
                        break;
                    case 'x':
                        channel.close();
                        return;
                }
            }
            catch(Exception ex) {
            }
        }
    }


    public void viewAccepted(View view) {
        System.out.println("-- VIEW: " + view);
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        DataOutput out=new DataOutputStream(output);
        synchronized(stocks) {
            System.out.println("-- returning " + stocks.size() + " stocks");
            Util.objectToStream(stocks,out);
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        DataInput in=new DataInputStream(input);
        Map<String,Double> new_state=(Map<String,Double>)Util.objectFromStream(in);
        System.out.println("-- received state: " + new_state.size() + " stocks");
        synchronized(stocks) {
            stocks.clear();
            stocks.putAll(new_state);
        }
    }



    private void getStock() throws IOException {
        String ticker=readString("Symbol");
        synchronized(stocks) {
            Double val=stocks.get(ticker);
            System.out.println(ticker + " is " + val);
        }
    }

    private void setStock() throws Exception {
        String ticker, val;
        ticker=readString("Symbol");
        val=readString("Value");
        RspList<Void> rsps=disp.callRemoteMethods(null,"_setStock",new Object[]{ticker,Double.parseDouble(val)},
                                                  new Class[]{String.class,double.class},RequestOptions.SYNC());
        System.out.println("rsps:\n" + rsps);
    }


    private void removeStock() throws Exception {
        String ticker=readString("Symbol");
        RspList<Void> rsps=disp.callRemoteMethods(null,"_removeStock",new Object[]{ticker},
                                                  new Class[]{String.class},RequestOptions.SYNC());
        System.out.println("rsps:\n" + rsps);
    }

    private void showStocks() {
        System.out.println("Stocks:");
        synchronized(stocks) {
            for(Map.Entry<String,Double> entry: stocks.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }


    private static String readString(String s) throws IOException {
        int c;
        boolean looping=true;
        StringBuilder sb=new StringBuilder();
        System.out.print(s + ": ");
        System.out.flush();
        System.in.skip(System.in.available());

        while(looping) {
            c=System.in.read();
            switch(c) {
                case -1:
                case '\n':
                case 13:
                    looping=false;
                    break;
                default:
                    sb.append((char)c);
                    break;
            }
        }

        return sb.toString();
    }


    public static void main(String[] args) throws Exception {
        String props="udp.xml";
        for(int i=0; i < args.length; i++) {
            if(args[i].equals("-props")) {
                props=args[++i];
                continue;
            }
            System.out.println("ReplicatedStockServer [-props <XML config file>]");
            return;
        }

        new ReplicatedStockServer().start(props);
    }


}
