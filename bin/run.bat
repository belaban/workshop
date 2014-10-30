@echo off

rem Configurable properties:

rem bind address, set the network interface to use for clustering traffic
rem set BIND_ADDR=192.168.1.5
rem set BIND_ADDR=match-interface:en.*
rem set BIND_ADDR=link_local

set BIND_ADDR=match-address:192.168.1.*

set MCAST_ADDR=232.5.5.5


set JG=.
set LIB=%JG%

set CP=%JG%\classes\;%JG%\conf\;%LIB%\*

set VMFLAGS=-Xmx500M -Xms500M

set LOG=-Dlog4j.configurationFile=conf\log4j2.xml

set FLAGS=-Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=%BIND_ADDR% -Djgroups.udp.mcast_addr=%MCAST_ADDR%

java -classpath %CP% %LOG% %VMFLAGS% %FLAGS% -Dcom.sun.management.jmxremote -Dresolve.dns=false %*
