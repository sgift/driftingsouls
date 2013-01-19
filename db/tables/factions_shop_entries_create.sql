CREATE TABLE `factions_shop_entries` (
  `id` smallint(5) unsigned NOT NULL auto_increment,
  `faction_id` int(11) NOT NULL default '-2',
  `type` tinyint(3) unsigned NOT NULL default '0',
  `resource` varchar(10) NOT NULL default '',
  `price` int(10) unsigned NOT NULL default '1',
	lpKosten INT NOT NULL DEFAULT 0,
  `availability` tinyint(3) unsigned NOT NULL default '0',
  `min_rank` INT NOT NULL DEFAULT '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `faction_id` (`faction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
