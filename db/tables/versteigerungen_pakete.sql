CREATE TABLE `versteigerungen_pakete` (
  `id` tinyint(3) unsigned NOT NULL auto_increment,
  `bieter` int(11) NOT NULL default '-2',
  `preis` mediumint(5) unsigned NOT NULL default '0',
  `tick` mediumint(5) unsigned NOT NULL default '0',
  `ships` text NOT NULL,
  `cargo` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='gtu - pakete';
 
ALTER TABLE versteigerungen_pakete ADD CONSTRAINT versteigerungen_pakete_fk_users FOREIGN KEY (bieter) REFERENCES users(id);