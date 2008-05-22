CREATE TABLE `portal_downloads` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(60) NOT NULL,
  `file` varchar(60) NOT NULL,
  `date` int(10) unsigned NOT NULL default '0',
  `description` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Downloads im Portal'; 
