CREATE TABLE `user_relations` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `target_id` int(11) NOT NULL default '0',
  `status` tinyint(3) unsigned NOT NULL default '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `user_id` (`user_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
