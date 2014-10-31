@echo off

rem Configurable properties:

rem bind address, set the network interface to use for clustering traffic
rem set BIND_ADDR=192.168.1.5
rem set BIND_ADDR=match-interface:en.*
rem set BIND_ADDR=site_local

rem ############# Change this ##########
set BIND_ADDR=match-address:192.168.1.*
rem ####################################

set MCAST_ADDR=232.5.5.5


set LIB=..\lib
set CLASSES=..\classes
set CONF=..\conf

set CP=%CLASSES%;%CONF%;%LIB%\*
set LOG=-Dlog4j.configurationFile=%CONF%/log4j2.xml


set JG_FLAGS=-Djgroups.bind_addr=%BIND_ADDR% -Djgroups.udp.mcast_addr=%MCAST_ADDR%
set JG_FLAGS=%JG_FLAGS% -Djava.net.preferIPv4Stack=true
set FLAGS=-server -Xmx600M -Xms600M
set FLAGS=%FLAGS% -XX:CompileThreshold=10000 -XX:ThreadStackSize=64K -XX:SurvivorRatio=8
set FLAGS=%FLAGS% -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=15
set FLAGS=%FLAGS% -Xshare:off
set JMX=-Dcom.sun.management.jmxremote

java -cp %CP% %DEBUG% %LOG% %GC% %JG_FLAGS% %FLAGS% %JMX%  %*
