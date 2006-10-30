CREATE TABLE `quests_answers` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `text` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Dialogantworten fuer Quests'; 
