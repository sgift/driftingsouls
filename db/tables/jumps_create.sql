CREATE TABLE `jumps` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `shipid` int(11) NOT NULL default '0',
  `x` smallint(5) unsigned NOT NULL default '0',
  `y` smallint(5) unsigned NOT NULL default '0',
  `system` tinyint(3) unsigned NOT NULL default '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `shipid` (`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Ziele von Sprungschiffen'; 
