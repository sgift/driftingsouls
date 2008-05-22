CREATE TABLE `quests_running` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `questid` mediumint(8) unsigned NOT NULL default '0',
  `userid` int(11) NOT NULL default '0',
  `execdata` BLOB NOT NULL,
  `uninstall` text,
  `statustext` varchar(100) NOT NULL default '',
  `publish` tinyint(3) unsigned NOT NULL default '0',
  `ontick` mediumint(8) unsigned default NULL,
  PRIMARY KEY  (`id`),
  KEY `questid` (`questid`,`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die laufenden Quests'; 
