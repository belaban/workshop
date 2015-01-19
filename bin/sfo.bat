
set SITE=sfo
set MCAST_ADDR=228.7.7.7
set MCAST_PORT=17000


run.bat -Dsite=%SITE% -Dmcast_addr=%MCAST_ADDR% -Dmcast_port=%MCAST_PORT% org.lab.ChatDemoRpc -props relay-local.xml %*

