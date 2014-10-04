#!/bin/sh
export DISPLAY=:99
cd /ds_tests/test/client
killall java
killall chromium-browser
mkdir -p log
rm log/*

PORT=9876
CHROMIUM=`which chromium-browser`
java -jar "test/lib/jstestdriver/JsTestDriver.jar" \
	 --port ${PORT} \
     --browserTimeout 20000 \
     --browser ${CHROMIUM} \
     --config "jsTestDriver.conf" \
     --basePath "." \
     --tests all \
     --reset \
     --testOutput "log" \
     --captureConsole
