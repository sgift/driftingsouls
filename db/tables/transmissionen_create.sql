CREATE TABLE `transmissionen` (
  `id` integer NOT NULL auto_increment,
  `gelesen` integer NOT NULL default '0',
  `sender` integer NOT NULL default '0',
  `empfaenger` integer NOT NULL default '0',
  `title` varchar(100) NOT NULL default '',
  `time` integer NOT NULL default '0',
  `ordner` int(10) unsigned NOT NULL default '0',
  `flags` integer NOT NULL default '0',
  `inhalt` text NOT NULL,
  `kommentar` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `empfaenger` (`empfaenger`,`gelesen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
