CREATE TABLE `fz` (
  `id` int(11) NOT NULL auto_increment,
  `col` int(11) NOT NULL default '0',
  `type` int(11) NOT NULL default '0',
  `forschung` int(11) NOT NULL default '0',
  `dauer` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `col` (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
