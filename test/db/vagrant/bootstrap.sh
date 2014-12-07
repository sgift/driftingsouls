#!/usr/bin/env bash
apt-get update
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password 12345'
sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password 12345'
apt-get install -y mysql-server ant

# Java 8
wget --no-check-certificate --no-cookies -nv --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u25-b17/jdk-8u25-linux-x64.tar.gz
tar -xzf jdk-8u25-linux-x64.tar.gz
rm jdk-8u25-linux-x64.tar.gz
mv jdk1.8.0_25 /usr/lib/jvm/
update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/jdk1.8.0_25/bin/java" 1
update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/lib/jvm/jdk1.8.0_25/bin/javaws" 1
update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/jdk1.8.0_25/bin/javac" 1
update-alternatives --install "/usr/bin/jar" "jar" "/usr/lib/jvm/jdk1.8.0_25/bin/jar" 1
update-alternatives --set "java" "/usr/lib/jvm/jdk1.8.0_25/bin/java"
update-alternatives --set "javac" "/usr/lib/jvm/jdk1.8.0_25/bin/javac"
update-alternatives --set "javaws" "/usr/lib/jvm/jdk1.8.0_25/bin/javaws"
update-alternatives --set "jar" "/usr/lib/jvm/jdk1.8.0_25/bin/jar"