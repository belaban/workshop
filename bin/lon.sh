#!/bin/bash

SITE=lon
MCAST_ADDR=228.5.5.5
MCAST_PORT=15000


`dirname $0`/run.sh -Dsite=$SITE -Dmcast_addr=$MCAST_ADDR -Dmcast_port=$MCAST_PORT org.lab.ChatDemoRpc -props relay-local.xml $*

