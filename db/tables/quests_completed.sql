CREATE TABLE `quests_completed` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `questid` mediumint(8) unsigned NOT NULL default '0',
  `userid` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `questid` (`questid`,`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Beendete Quests'; 

ALTER TABLE quests_completed ADD CONSTRAINT quests_completed_fk_users FOREIGN KEY (userid) REFERENCES users(id);
ALTER TABLE quests_completed ADD CONSTRAINT quests_completed_fk_quests FOREIGN KEY (questid) REFERENCES quests(id);