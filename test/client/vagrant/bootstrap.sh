#!/usr/bin/env bash
apt-get update
apt-get install -y firefox chromium-browser openjdk-7-jre xvfb
cp /vagrant/Xvfb /etc/init.d/.
update-rc.d Xvfb defaults
chmod +x /etc/init.d/Xvfb
service Xvfb start