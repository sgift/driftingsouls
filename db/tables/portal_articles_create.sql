CREATE TABLE `portal_articles` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `title` varchar(80) NOT NULL default '',
  `author` varchar(40) NOT NULL default '',
  `article` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Portal -> Artikel'; 
