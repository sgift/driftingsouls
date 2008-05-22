CREATE TABLE `skn` (
  `post` int(6) NOT NULL auto_increment,
  `userid` int(6) NOT NULL default '0',
  `time` int(10) unsigned NOT NULL default '0',
  `name` varchar(255) NOT NULL default '',
  `head` varchar(240) NOT NULL default '',
  `text` text NOT NULL,
  `pic` int(11) NOT NULL default '0',
  `allypic` int(11) NOT NULL default '0',
  `tick` mediumint(8) unsigned NOT NULL default '0',
  `channel` int(10) unsigned NOT NULL default '1',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`post`),
  KEY `channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
