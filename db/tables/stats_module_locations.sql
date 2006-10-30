CREATE TABLE `stats_module_locations` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `item_id` smallint(5) unsigned NOT NULL default '0',
  `locations` varchar(85) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
