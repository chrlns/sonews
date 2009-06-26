#!/bin/bash
SCRIPTROOT=$(dirname $0)
CLASSPATH=$SCRIPTROOT/lib/jchart2d.jar:\
$SCRIPTROOT/lib/sonews.jar:\
$SCRIPTROOT/lib/servlet-api-2.5.jar
ARG0=org.sonews.web.SonewsServlet@sonews
ARG1=org.sonews.web.SonewsConfigServlet@sonews/config
ARG2=org.sonews.web.SonewsPeerServlet@sonews/peer
ARG3=org.sonews.web.SonewsChartServlet@sonews/chart
java -cp $CLASSPATH org.sonews.kitten.Main -s $ARG0 -s $ARG1 -s $ARG2 -s $ARG3
