#!/bin/bash

### Configurable properties:

## bind address, set the network interface to use for clustering traffic
#BIND_ADDR=192.168.1.5
#BIND_ADDR=match-interface:en.*
#BIND_ADDR=link_local

################# CHANGE THIS ##############################
BIND_ADDR=match-address:192.168.1.*
############################################################




LIB=`dirname $0`/../lib
CLASSES=`dirname $0`/../classes
CONF=`dirname $0`/../conf

CP=$CLASSES:$CONF:$LIB/*

if [ -f $CONF/log4j.properties ]; then
    LOG="-Dlog4j.configuration=file:$CONF/log4j.properties"
fi;

if [ -f $CONF/log4j2.xml ]; then
    LOG="$LOG -Dlog4j.configurationFile=$CONF/log4j2.xml"
fi;

if [ -f $CONF/logging.properties ]; then
    LOG="$LOG -Djava.util.logging.config.file=$CONF/logging.properties"
fi;

JG_FLAGS="-Djgroups.bind_addr=${BIND_ADDR}"
JG_FLAGS="$JG_FLAGS -Djava.net.preferIPv4Stack=true"
FLAGS="-server -Xmx600M -Xms600M"
FLAGS="$FLAGS -XX:CompileThreshold=10000 -XX:ThreadStackSize=64K -XX:SurvivorRatio=8"
FLAGS="$FLAGS -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=15"
FLAGS="$FLAGS -Xshare:off"
# FLAGS="$FLAGS -XX:+UseStringDeduplication" ## JDK 8u20
#GC="-XX:+UseG1GC" ## use at least JDK 8
GC="-XX:+UseParNewGC -XX:+UseConcMarkSweepGC" ## concurrent mark and sweep (CMS) collector

# JMX="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7777 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
JMX="-Dcom.sun.management.jmxremote"
#EXPERIMENTAL="-XX:+UseFastAccessorMethods -XX:+UseTLAB"

#EXPERIMENTAL="$EXPERIMENTAL -XX:+DoEscapeAnalysis -XX:+EliminateLocks -XX:+UseBiasedLocking"
EXPERIMENTAL="$EXPERIMENTAL -XX:+EliminateLocks -XX:+UseBiasedLocking"

#DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000"
#JMC="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder"

java -cp $CP $DEBUG $LOG $GC $JG_FLAGS $FLAGS $EXPERIMENTAL $JMX $JMC  $*

