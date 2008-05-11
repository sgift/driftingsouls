CREATE TABLE `stats_ships` (
  `tick` int(10) unsigned NOT NULL default '0',
  `shipcount` int(10) unsigned NOT NULL default '0',
  `crewcount` int(10) unsigned NOT NULL default '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
