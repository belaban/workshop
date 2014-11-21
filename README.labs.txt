
Labs
====
There are 2 labs: a chat and a replicated hashmap.

Both labs should be created under src/org/lab, e.g. src/org/lab/ChatDemo.java.

To work on a demo, it is recommended to use an IDE. The sources are in ./lib, e.g. ./lib/jgroups-3.6.0.Final-sources.jar.

To compile, simply run 'ant' in the root directory (workshop).

To run a demo (e.g. ChatDemo): bin/run.sh org.lab.ChatDemo <args>.




ChatDemo
--------
This demo reads a line of input from stdin and sends it to all cluster nodes. Use Util.readLine(System.in) to read input.

The chat should print when new members join the cluster, or members leave or crash.

It should also print received chat messages to stdout.

The configuration file to be used should be config.xml (which is on the classpath, in ./conf/config.xml).

A chat node can be run with: bin/run.sh org.lab.ChatDemo -props config.xml -name <name>




ReplicatedStockServer
---------------------
A hashmap of stocks (strings) and their current values (doubles), replicated across a cluster. Any change to a stock
should be via a remote procedure call to all cluster nodes (using RpcDispatcher).

There's a non-replicated StockServer sample program (containing the basic structire and event loop).
This can be copied and renamed to ReplicatedStockServer.

There should be methods _setStock(String name, double val) and _removeStock(String name) which update or remove stocks
respectively. These are called via RpcDispatcher.

A node has an event loop which looks like this:
[1] Show stocks [2] Get quote [3] Set quote [4] Remove quote [x] Exit

[1] Displays all stocks in the local hashmap.
[2] Gets a value for a given stock (also local)
[3] Sets a value for a stock. This triggers an RpcDispatcher.callRemoteMethods() call, which calls _setStock() in all
    cluster nodes. On invocation of _setStock(), each cluster node adds the shipped data into its hashmap.
[4] Removes a stock from all hashmaps of all cluster nodes. Also an RPC calling _removeStock();

We'll create a JChannel, then an RpcDispatcher over it, and finally call JChannel.connect(clustername).
The, JChannel.getState() needs to be called, to transfer state (the contents of the local hashmap) to the newly
joined node. This looks roughly as follows:
 channel=new JChannel("config.xml);
 disp=new RpcDispatcher(channel, this, this, this);
 channel.connect("stocks");
 disp.start();
 channel.getState(null, 30000); // fetches the state from the coordinator

Methods getState() and setState() of ReceiverAdapter need to be overwritten to implement the state transfer. Also,
pbcast.STATE needs to be on the stack (config.xml already contains it).

The server can be run with bin/run.sh org.lab.ReplicatedStockServer -props config.xml.
