CREATE TABLE `nebel` (
  `x` int(11) NOT NULL default '0',
  `y` int(11) NOT NULL default '0',
  `system` int(11) NOT NULL default '0',
  `type` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`system`,`x`,`y`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
