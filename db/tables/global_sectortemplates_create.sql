CREATE TABLE `global_sectortemplates` (
  `id` varchar(30) NOT NULL default '',
  `x` smallint(5) unsigned NOT NULL default '0',
  `y` smallint(5) unsigned NOT NULL default '0',
  `w` smallint(5) unsigned NOT NULL default '0',
  `h` smallint(5) unsigned NOT NULL default '0',
  `scriptid` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Sectortemplatemanager-IDs'; 
