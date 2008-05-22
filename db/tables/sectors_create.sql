CREATE TABLE `sectors` (
  `system` tinyint(4) NOT NULL default '1',
  `x` smallint(5) NOT NULL default '1',
  `y` smallint(5) NOT NULL default '1',
  `objects` int(11) NOT NULL default '0',
  `onenter` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`system`,`x`,`y`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
