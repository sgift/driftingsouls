CREATE TABLE `stats_cargo` (
  `tick` int(11) NOT NULL auto_increment,
  `cargo` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Cargo-Statistik (Gesamtcargo)'; 
