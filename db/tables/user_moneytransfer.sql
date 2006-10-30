CREATE TABLE `user_moneytransfer` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `from` int(11) NOT NULL default '0',
  `to` int(11) NOT NULL default '0',
  `time` int(10) unsigned NOT NULL default '0',
  `count` int(10) unsigned NOT NULL default '0',
  `text` text NOT NULL,
  `fake` tinyint(3) unsigned NOT NULL default '0',
  `type` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `from` (`from`,`to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='RE-Ueberweisungen'; 
