CREATE TABLE `gtu_warenkurse` (
  `place` varchar(10) NOT NULL default 'asti',
  `name` varchar(255) NOT NULL default '',
  `kurse` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`place`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
