CREATE TABLE `werften` (
  `id` int(11) NOT NULL auto_increment,
  `type` smallint(6) NOT NULL default '0',
  `building` int(11) NOT NULL default '0',
  `item` smallint(6) NOT NULL default '-1',
  `remaining` tinyint(4) NOT NULL default '0',
  `flagschiff` tinyint(1) unsigned NOT NULL default '0',
  `col` int(11) NOT NULL default '0',
  `shipid` int(11) NOT NULL default '0',
  `linked` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `col` (`col`),
  KEY `shipid` (`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;