#!/bin/bash
cd `dirname $0`/..
NEWSROOT=`pwd`
CLASSPATH=$NEWSROOT/lib/sonews.jar:\
$NEWSROOT/lib/sonews-helpers.jar:\
$NEWSROOT/lib/mysql-connector-java.jar:\
$NEWSROOT/lib/glassfish-mail.jar:\
$NEWSROOT/lib/postgresql.jar

LOGFILE=/var/log/sonews.log
PIDFILE=/var/pid/sonews.pid
ARGS=$@

MAINCLASS=org.sonews.Main
JAVA=$JAVA_HOME/bin/java

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
