
Instructor notes
================

Attendees should have Ant installed.

To create the demos/labs, run `ant`. This will create dirs `lib` and `classes`. Ant will first download all
required JARs into `lib` and then compile the labs and demos into `classes`.

If no internet connection is available, make sure that the distribution includes the full `lib` directory. Without
the JARs the demos and labs won't work.

The slides are written in asciidoc and can be rendered via: asciidoc --backend slidy <file>.adoc. As an alternative,
the reveal.js or deck.js backend can be used, but this currently needs to be installed (google for how-to).

Java (7+) and ant are required to prepare the distribution. Running 'ant' in the main dir (workshop) will
download ivy and other required libs into ./lib and compile the local code into ./classes.

Before doing this, src/org/lab/ChatDemo.java and ReplicatedStockServer.java should be removed (they're the solutions).

To run a solution (e.g. ChatDemo), ./bin/run.sh org.lab.ChatDemo <args> can be used.

It is best to always use ./conf/config.xml as configuration. This ./conf dir is on the classpath and config.xml
should be modified if needed.

The ./bin dir contains various scripts to run labs and demos.