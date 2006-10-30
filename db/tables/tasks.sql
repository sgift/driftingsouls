CREATE TABLE `tasks` (
  `taskid` varchar(60) NOT NULL default '',
  `type` tinyint(3) unsigned NOT NULL default '0',
  `time` int(10) unsigned NOT NULL default '0',
  `timeout` smallint(5) unsigned NOT NULL default '0',
  `data1` varchar(60) NOT NULL default '',
  `data2` varchar(60) NOT NULL default '',
  `data3` varchar(60) NOT NULL default '',
  PRIMARY KEY  (`taskid`),
  KEY `type` (`type`,`time`,`data1`,`data2`,`data3`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='DS2-Tasks'; 
