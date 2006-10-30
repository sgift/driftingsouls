CREATE TABLE `gtu_zwischenlager` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `posten` int(10) unsigned NOT NULL default '0',
  `user1` int(11) NOT NULL default '0',
  `user2` int(11) NOT NULL default '0',
  `cargo1` text NOT NULL,
  `cargo1need` text NOT NULL,
  `cargo2` text NOT NULL,
  `cargo2need` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `posten` (`posten`,`user1`,`user2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
