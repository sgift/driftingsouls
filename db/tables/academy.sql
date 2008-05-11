CREATE TABLE `academy` (
  `col` int(11) NOT NULL default '0',
  `train` tinyint(4) NOT NULL default '0',
  `remain` tinyint(4) NOT NULL default '0',
  `upgrade` varchar(10) NOT NULL default '',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE academy ADD CONSTRAINT academy_fk_bases FOREIGN KEY (col) REFERENCES bases(id);