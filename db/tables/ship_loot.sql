CREATE TABLE `ship_loot` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `shiptype` int(11) NOT NULL default '0',
  `owner` int(11) NOT NULL default '0',
  `targetuser` int(11) NOT NULL default '0',
  `chance` int(10) unsigned NOT NULL default '0',
  `resource` varchar(16) NOT NULL default '',
  `count` smallint(5) unsigned NOT NULL default '1',
  `totalmax` smallint(6) NOT NULL default '-1',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `shiptype` (`shiptype`),
  KEY `owner` (`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Schiff-Loot-Table'; 

ALTER TABLE ship_loot ADD CONSTRAINT ship_loot_fk_users1 FOREIGN KEY (owner) REFERENCES users(id);
ALTER TABLE ship_loot ADD CONSTRAINT ship_loot_fk_users2 FOREIGN KEY (targetuser) REFERENCES users(id);