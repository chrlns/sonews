#!/bin/bash
SCRIPTROOT=$(pwd)
CLASSPATH=$SCRIPTROOT/lib/sonews.jar:\
$SCRIPTROOT/lib/mysql-connector-java.jar:\
$SCRIPTROOT/lib/glassfish-mail.jar:\
$SCRIPTROOT/lib/postgresql.jar

LOGFILE=sonews.log
PIDFILE=sonews.pid
ARGS=$@

MAINCLASS=org.sonews.daemon.Main
JAVA=java

case "$1" in
  start)
    echo "Starting sonews Newsserver..."
    $JAVA -classpath $CLASSPATH $MAINCLASS $ARGS &> $LOGFILE &
    echo $! > $PIDFILE
    ;;
  stop)
    echo "Stopping sonews Newsserver..."
    PID=`cat $PIDFILE`
    kill -15 $PID
    ;;
  setup)
    $JAVA -classpath $CLASSPATH org.sonews.util.DatabaseSetup
    ;;
  purge)
    $JAVA -classpath $CLASSPATH org.sonews.util.Purger
    ;;

  *)
    echo "Usage: sonews [start|stop|setup|purge]"
esac
