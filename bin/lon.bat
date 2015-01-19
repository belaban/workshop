
set SITE=lon
set MCAST_ADDR=228.5.5.5
set MCAST_PORT=15000


run.bat -Dsite=%SITE% -Dmcast_addr=%MCAST_ADDR% -Dmcast_port=%MCAST_PORT% org.lab.ChatDemo -props relay-local.xml %*

