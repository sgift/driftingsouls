#!/bin/sh

cd vagrant
if [ ! -f .vm_v2 ]; then
	rm .vm_v*
	vagrant destroy -f
	vagrant box add chef/ubuntu-14.04 --provider=virtualbox
	vagrant reload --provision
	touch .vm_v2
fi
vagrant up --provider=virtualbox
vagrant ssh -c 'sh /vagrant/run.sh'
vagrant suspend
cd ..