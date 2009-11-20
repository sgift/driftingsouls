CREATE TABLE `ammo` (
  `id` int(11) unsigned NOT NULL auto_increment,
  `name` varchar(30) NOT NULL default '',
  `description` tinytext NOT NULL,
  `type` varchar(10) NOT NULL default 'rak',
  `damage` smallint(6) unsigned NOT NULL default '0',
  `shielddamage` smallint(6) unsigned NOT NULL default '0',
  `subdamage` smallint(5) unsigned NOT NULL default '0',
  `trefferws` smallint(6) unsigned NOT NULL default '0',
  `smalltrefferws` tinyint(3) unsigned NOT NULL default '50',
  `torptrefferws` tinyint(3) unsigned NOT NULL default '0',
  `subws` smallint(5) unsigned NOT NULL default '0',
  `shotspershot` tinyint(3) unsigned NOT NULL default '1',
  `areadamage` tinyint(3) unsigned NOT NULL default '0',
  `destroyable` float(3,2) unsigned NOT NULL default '0.00',
  `flags` int(10) unsigned NOT NULL default '0',
  `picture` varchar(20) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Raketen, Flak, Torpedomunition'; 
