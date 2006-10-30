CREATE TABLE `quests_text` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `text` text NOT NULL,
  `picture` varchar(30) NOT NULL default '',
  `comment` varchar(40) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 PACK_KEYS=0 COMMENT='Dialogtexte fuer Quests'; 
