import org.jgroups.*;
import org.jgroups.util.*;
import org.jgroups.blocks.*;
import org.jgroups.jmx.*;
import org.misc.*;
import java.nio.*;
import java.nio.channels.*;

println "JGroups version: " + Version.description
println "Bind addr:" + System.getenv("BIND_ADDR")

def mult(x) {x*x}


def createChannel(String name) {ch=new JChannel("config.xml").name(name); ch.setReceiver(new SampleReceiver(name)); ch}

def register(JChannel ch) {JmxConfigurator.registerChannel(ch, Util.getMBeanServer(), "jgroups", ch.getClusterName(), true)}

