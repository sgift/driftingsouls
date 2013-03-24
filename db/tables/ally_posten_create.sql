CREATE TABLE `ally_posten` (
  `id` int(11) NOT NULL auto_increment,
  `ally` INTEGER NOT NULL,
  `name` varchar(255) NOT NULL,
  `version` INTEGER NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die verschiedenen Posten der Allys'; 
