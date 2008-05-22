CREATE TABLE `sessions` (
  `id` int(11) NOT NULL default '0',
  `session` varchar(36) NOT NULL default '',
  `ip` text NOT NULL,
  `lastaction` int(10) unsigned NOT NULL default '0',
  `actioncounter` int(10) NOT NULL default '0',
  `usegfxpak` tinyint(3) unsigned NOT NULL default '1',
  `tick` tinyint(3) unsigned NOT NULL default '0',
  `attach` varchar(36) default NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY (`session`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
