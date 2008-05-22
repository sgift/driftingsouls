CREATE TABLE `battles_ships` (
  `shipid` int(11) NOT NULL default '0',
  `battleid` smallint(5) unsigned NOT NULL default '0',
  `side` tinyint(3) unsigned NOT NULL default '0',
  `hull` mediumint(8) unsigned NOT NULL default '0',
  `shields` mediumint(8) unsigned NOT NULL default '0',
  `ablativeArmor` int(11) unsigned NOT NULL default '0',
  `engine` tinyint(3) unsigned NOT NULL default '0',
  `weapons` tinyint(3) unsigned NOT NULL default '0',
  `comm` tinyint(3) unsigned NOT NULL default '0',
  `sensors` tinyint(3) unsigned NOT NULL default '0',
  `action` smallint(5) unsigned NOT NULL default '0',
  `count` tinyint(3) unsigned NOT NULL default '1',
  `newcount` tinyint(3) unsigned NOT NULL default '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`shipid`),
  KEY `battleid` (`battleid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Schiffsdaten in Schlachten'; 
