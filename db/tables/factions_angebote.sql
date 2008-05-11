CREATE TABLE `factions_angebote` (
  `id` tinyint(3) unsigned NOT NULL auto_increment,
  `faction` int(11) NOT NULL default '-2',
  `title` varchar(40) NOT NULL default '',
  `image` varchar(100) NOT NULL default '',
  `description` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='GTU-Festpreisangebote'; 
