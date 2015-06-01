import org.jgroups.*;
import org.jgroups.util.*;
import org.jgroups.blocks.*;
import org.jgroups.jmx.*;
import org.misc.*;

println "JGroups version: " + Version.description

def mult(x) {x*x}


def createChannel(String name) {ch=new JChannel("config.xml").name(name); ch.setReceiver(new SampleReceiver(name)); ch}

def register(JChannel ch) {JmxConfigurator.registerChannel(ch, Util.getMBeanServer(), "jgroups", ch.getClusterName(), true)}

