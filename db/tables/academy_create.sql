CREATE TABLE `academy` (
  `id` int(11) NOT NULL auto_increment,
  `train` tinyint(4) NOT NULL default '0',
  `remain` tinyint(4) NOT NULL default '0',
  `upgrade` varchar(10) NOT NULL default '',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
