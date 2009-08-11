CREATE TABLE `sessions` (
  `userId` int(11) NOT NULL default '0',
  `token` varchar(36) NOT NULL default '',
  `usegfxpack` tinyint(3) unsigned NOT NULL default '1',
  `version` int(10) unsigned not null default '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
