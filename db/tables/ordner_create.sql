CREATE TABLE `ordner` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `playerid` int(11) NOT NULL,
  `flags` tinyint(3) unsigned NOT NULL default '0',
  `parent` int(11) NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `playerid` (`playerid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
