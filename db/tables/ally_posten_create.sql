CREATE TABLE `ally_posten` (
  `id` int(11) NOT NULL auto_increment,
  `ally` int(11) NOT NULL default '0',
  `name` varchar(70) NOT NULL default 'Kein Name',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die verschiedenen Posten der Allys'; 
