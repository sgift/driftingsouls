#!/bin/sh
cd /ds
rm build/test-output/db-test.xml
echo "drop database ds2;" | mysql -u root --password=12345
echo "create database ds2" | mysql -u root --password=12345
mysql -u root --password=12345 ds2 < src/db/initial_version.sql
if [ "$?" -ne 0 ]; then
	echo "<testsuite tests='1'><testcase classname='net.driftingsouls.ds2.server.dbtest.SetupDatabase' name='SetupDatabaseViaShell'><failure type='MysqlError'>initial_schema.sql konnte nicht importiert werden</failure></testcase></testsuite>" > build/test-output/db-test.xml
	exit 1
fi

ant test-db-run