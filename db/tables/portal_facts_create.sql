CREATE TABLE `portal_facts` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `class` varchar(16) NOT NULL default '',
  `title` varchar(64) NOT NULL default '',
  `text` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `class` (`class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Kleine Artikel zu diversen Dingen aus FS und DS (Schiffe/Ges';
