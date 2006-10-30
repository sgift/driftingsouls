CREATE TABLE `stats_cargo` (
  `tick` int(11) NOT NULL auto_increment,
  `cargo` text NOT NULL,
  PRIMARY KEY  (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Cargo-Statistik (Gesamtcargo)'; 
