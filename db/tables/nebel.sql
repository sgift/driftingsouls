CREATE TABLE `nebel` (
  `id` int(11) NOT NULL auto_increment,
  `x` int(11) NOT NULL default '0',
  `y` int(11) NOT NULL default '0',
  `system` int(11) NOT NULL default '0',
  `type` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `coords` (`x`,`y`,`system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
