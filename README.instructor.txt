
Instructor notes
================

The slides are written in asciidoc and can be rendered via: asciidoc --backend slidy <file>.adoc. As an alternative,
the reveal.js or deck.js backend can be used, but this currently needs to be installed (google for how-to).

Java (7+) and ant are required to prepare the distribution. Running 'ant' in the main dir (workshop) will
download ivy and other required libs into ./lib and compile the local code into ./classes.

Before doing this, src/org/lab/ChatDemo.java and ReplicatedStockServer.java should be removed (they're the solutions).

To run a solution (e.g. ChatDemo), ./bin/run.sh org.lab.ChatDemo <args> can be used.

It is best to always use ./conf/config.xml as configuration. This file is on the classpath (with run.sh | run.bat) and
should be modified if needed.

