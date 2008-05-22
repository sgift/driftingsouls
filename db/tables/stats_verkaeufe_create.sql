CREATE TABLE `stats_verkaeufe` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `tick` mediumint(8) unsigned NOT NULL default '0',
  `place` varchar(10) NOT NULL default '',
  `system` tinyint(3) NOT NULL default '1',
  `stats` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `place` (`place`,`system`),
  KEY `tick` (`tick`),
  KEY `system` (`system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Verkaufsstatistik'; 
