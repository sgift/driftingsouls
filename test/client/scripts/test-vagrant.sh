#!/bin/sh

cd vagrant
vagrant box add chef/ubuntu-13.10 --provider=virtualbox
vagrant up --provider=virtualbox
vagrant ssh -c 'sh /vagrant/run.sh'
vagrant suspend
cd ..