
set SITE=nyc
set MCAST_ADDR=228.6.6.6
set MCAST_PORT=16000


run.bat -Dsite=%SITE% -Dmcast_addr=%MCAST_ADDR% -Dmcast_port=%MCAST_PORT% org.lab.ChatDemoRpc -props relay-local.xml %*

