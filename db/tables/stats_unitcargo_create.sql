CREATE TABLE `stats_unitcargo` (
  `tick` int(11) NOT NULL auto_increment,
  `unitcargo` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die UnitCargo-Statistik (Gesamtcargo)'; 
