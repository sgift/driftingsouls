CREATE TABLE `ship_loot` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `shiptype` int(11) NOT NULL default '0',
  `owner` int(11) NOT NULL default '0',
  `targetuser` int(11) NOT NULL default '0',
  `chance` int(10) unsigned NOT NULL default '0',
  `resource` varchar(16) NOT NULL default '',
  `count` smallint(5) unsigned NOT NULL default '1',
  `totalmax` smallint(6) NOT NULL default '-1',
  PRIMARY KEY  (`id`),
  KEY `shiptype` (`shiptype`),
  KEY `owner` (`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Schiff-Loot-Table'; 
