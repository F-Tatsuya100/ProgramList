#! /bin/sh
java -Xmx512M -cp .:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
#jdb -Xmx512M -classpath .:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
