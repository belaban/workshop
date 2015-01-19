#!/bin/bash

SITE=sfo
MCAST_ADDR=228.7.7.7
MCAST_PORT=17000


`dirname $0`/run.sh -Dsite=$SITE -Dmcast_addr=$MCAST_ADDR -Dmcast_port=$MCAST_PORT org.lab.ChatDemoRpc -props relay-local.xml $*

