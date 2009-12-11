CREATE TABLE `sessions` (
  `id` int(11) NOT NULL auto_increment,
  `userId` int(11) NOT NULL default '0',
  `token` varchar(36) NOT NULL default '',
  `usegfxpack` tinyint(3) unsigned NOT NULL default '1',
  `tick` int(11) unsigned,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
