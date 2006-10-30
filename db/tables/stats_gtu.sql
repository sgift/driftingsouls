CREATE TABLE `stats_gtu` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `username` varchar(255) NOT NULL default '',
  `userid` int(11) NOT NULL default '-2',
  `mtype` tinyint(3) unsigned NOT NULL default '0',
  `type` text NOT NULL,
  `preis` bigint(20) unsigned NOT NULL default '1',
  `owner` int(11) NOT NULL default '-2',
  `ownername` varchar(255) NOT NULL default 'Galtracorp Unlimited',
  `gtugew` double NOT NULL default '100',
  PRIMARY KEY  (`id`),
  KEY `preis` (`preis`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='GTU Top10 Gebote'; 
