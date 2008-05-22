CREATE TABLE `surveys` (
  `id` smallint(5) unsigned NOT NULL auto_increment,
  `name` varchar(120) NOT NULL default '',
  `timeout` tinyint(3) unsigned NOT NULL default '49',
  `minid` int(11) NOT NULL default '-99',
  `maxid` int(11) NOT NULL default '99999',
  `mintime` int(10) unsigned NOT NULL default '0',
  `maxtime` int(10) unsigned NOT NULL default '3333333333',
  `enabled` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
