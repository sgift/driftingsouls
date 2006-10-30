CREATE TABLE `weaponfactory` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `col` int(10) unsigned NOT NULL default '0',
  `count` tinyint(3) unsigned NOT NULL default '0',
  `produces` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `col` (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Waffenfabriken'; 
