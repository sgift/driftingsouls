CREATE TABLE `inttutorial` (
  `id` tinyint(3) unsigned NOT NULL auto_increment,
  `reqbase` tinyint(3) unsigned NOT NULL default '0',
  `reqship` tinyint(3) unsigned NOT NULL default '0',
  `reqname` tinyint(3) unsigned NOT NULL default '0',
  `reqsheet` tinyint(3) unsigned NOT NULL default '0',
  `headimg` varchar(200) NOT NULL default '',
  `text` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Seiten fuer das interaktive Tutorial (Uebersicht)'; 
