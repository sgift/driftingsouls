CREATE TABLE `logging` (
  `id` bigint(20) unsigned NOT NULL auto_increment,
  `type` varchar(10) NOT NULL default '',
  `user_id` mediumint(9) NOT NULL default '0',
  `time` int(10) unsigned NOT NULL default '0',
  `source` varchar(12) NOT NULL default '',
  `target` varchar(12) NOT NULL default '',
  `data` text,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COMMENT='Alg. Logging'; 
