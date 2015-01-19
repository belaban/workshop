#!/bin/bash

SITE=nyc
MCAST_ADDR=228.6.6.6
MCAST_PORT=16000


`dirname $0`/run.sh -Dsite=$SITE -Dmcast_addr=$MCAST_ADDR -Dmcast_port=$MCAST_PORT org.lab.ChatDemo -props relay-local.xml $*

