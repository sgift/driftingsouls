CREATE TABLE `ally_posten` (
  `id` int(11) NOT NULL auto_increment,
  `ally` int(11) NOT NULL default '0',
  `name` varchar(70) NOT NULL default 'Kein Name',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die verschiedenen Posten der Allys'; 

ALTER TABLE ally_posten ADD CONSTRAINT ally_posten_fk_ally FOREIGN KEY (ally) REFERENCES ally(id);