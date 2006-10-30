CREATE TABLE `academy` (
  `id` int(11) NOT NULL auto_increment,
  `col` int(11) NOT NULL default '0',
  `train` tinyint(4) NOT NULL default '0',
  `remain` tinyint(4) NOT NULL default '0',
  `upgrade` varchar(10) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `col` (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
