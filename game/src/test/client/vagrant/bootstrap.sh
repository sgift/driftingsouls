#!/usr/bin/env bash
apt-get update
apt-get install -y chromium-browser xvfb
cp /vagrant/Xvfb /etc/init.d/.
update-rc.d Xvfb defaults
chmod +x /etc/init.d/Xvfb
service Xvfb start

apt-get install -y nodejs npm

cd /ds_tests/test/client/karma
#npm install
npm install -g karma-cli