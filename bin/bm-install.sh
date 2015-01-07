
#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "bm-install.sh PID"
    echo ""
    exit
fi

PID=$1
LIB=`dirname $0`/../lib
TOOLS=tools.jar

if [ -e $LIB/$TOOLS ]; then
    echo "$LIB/$TOOLS exists"
else
    echo "$LIB/$TOOLS doesn't exist, copying $JAVA_HOME/lib/$TOOLS --> $LIB/"
    # we also need a tools jar from JAVA_HOME
    if [ -z "$JAVA_HOME" ]; then
        echo "please set JAVA_HOME"
        exit
    fi
    cp $JAVA_HOME/lib/$TOOLS $LIB/
fi

`dirname $0`/run.sh org.jboss.byteman.agent.install.Install $1




