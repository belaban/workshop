import org.jgroups.*;
import org.jgroups.Version
import org.jgroups.jmx.JmxConfigurator
import org.jgroups.util.Util
import org.misc.SampleReceiver

println "JGroups version: " + Version.description
println "Bind addr:" + System.getenv("BIND_ADDR")

def mult(x) {x*x}


def createChannel(String name) {ch=new JChannel("config.xml").name(name).setReceiver(new SampleReceiver(name)); ch}

def register(JChannel ch) {JmxConfigurator.registerChannel(ch, Util.getMBeanServer(), "jgroups", ch.getClusterName(), true)}

static def sendMany(JChannel ch) {
  (0..100).forEach{i -> ch.send(null, i)}
}
