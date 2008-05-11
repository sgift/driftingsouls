CREATE TABLE `versteigerungen` (
  `id` int(11) NOT NULL auto_increment,
  `mtype` tinyint(3) unsigned NOT NULL default '0',
  `type` varchar(120) NOT NULL default '',
  `tick` int(11) NOT NULL default '0',
  `preis` int(11) NOT NULL default '0',
  `bieter` int(11) NOT NULL default '-2',
  `owner` int(11) NOT NULL default '-2',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE versteigerungen ADD CONSTRAINT versteigerungen_fk_users FOREIGN KEY (bieter) REFERENCES users(id);
