CREATE TABLE `user_values` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `name` varchar(60) NOT NULL default '',
  `value` TEXT NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `id` (`user_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
