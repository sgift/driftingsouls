CREATE TABLE `versteigerungen_pakete` (
  `id` tinyint(3) unsigned NOT NULL auto_increment,
  `bieter` int(11) NOT NULL default '-2',
  `preis` mediumint(5) unsigned NOT NULL default '0',
  `tick` mediumint(5) unsigned NOT NULL default '0',
  `ships` text NOT NULL,
  `cargo` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='gtu - pakete';
 
