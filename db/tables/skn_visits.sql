CREATE TABLE `skn_visits` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `user` int(11) NOT NULL default '0',
  `channel` int(11) unsigned NOT NULL default '0',
  `time` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `user` (`user`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Com-Net visits'; 

ALTER TABLE skn_visits ADD CONSTRAINT skn_visits_fk_users FOREIGN KEY (user) REFERENCES users(id);
ALTER TABLE skn_visits ADD CONSTRAINT skn_visits_fk_skn_channels FOREIGN KEY (channel) REFERENCES skn_channels(id);