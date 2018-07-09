#!/bin/bash

cd karma
export CHROME_BIN=`which chromium-browser`
nodejs ./node_modules/karma/bin/karma start ds.conf.js --single-run
cd ..