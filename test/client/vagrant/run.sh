#!/bin/sh
export DISPLAY=:99
cd /ds_tests/test/client
killall nodejs
killall chromium-browser
killall firefox
mkdir -p log
rm log/*

cd karma
export CHROME_BIN=`which chromium-browser`
nodejs ./node_modules/karma/bin/karma start ds.conf.js --single-run --browsers Chrome