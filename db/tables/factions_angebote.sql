CREATE TABLE `factions_angebote` (
  `id` tinyint(3) unsigned NOT NULL auto_increment,
  `faction` int(11) NOT NULL default '-2',
  `title` varchar(40) NOT NULL default '',
  `image` varchar(100) NOT NULL default '',
  `description` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='GTU-Festpreisangebote'; 
