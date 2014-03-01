CREATE TABLE `versteigerungen` (
  `id` integer NOT NULL auto_increment,
  `mtype` tinyint(3) unsigned NOT NULL default '0',
  `type` varchar(120) NOT NULL default '',
  `tick` integer NOT NULL default '0',
  `preis` integer NOT NULL default '0',
  `bieter` integer NOT NULL default '-2',
  `owner` integer NOT NULL default '-2',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
