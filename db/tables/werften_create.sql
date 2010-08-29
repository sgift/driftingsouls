CREATE TABLE `werften` (
  `id` int(11) NOT NULL auto_increment,
  `type` smallint(6) NOT NULL default '0',
  `flagschiff` tinyint(1) unsigned NOT NULL default '0',
  `shipid` int(11) default null,
  `linked` int(11) default null,
  `linkedWerft` int(11) default null,
  `komplex` tinyint(1) NOT NULL default '0', 
  `version` int(10) unsigned not null default '0',
  `werfttype` CHAR(1) NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `shipid` (`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
