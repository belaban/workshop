package org.lab;

import org.jgroups.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StockServer {
    private final Map<String,Double> stocks=new HashMap<String,Double>();


    private void start() throws Exception {
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
                        return;
                }
            }
            catch(Exception ex) {
            }
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
        stocks.put(ticker,Double.parseDouble(val));
    }


    private void removeStock() throws Exception {
        String ticker=readString("Symbol");
        stocks.remove(ticker);
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
        new StockServer().start();
    }


}
