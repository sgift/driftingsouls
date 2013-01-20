CREATE TABLE `stat_aktive_spieler` (
  `tick` int(11) NOT NULL auto_increment,
	sehrAktiv int(11) NOT NULL DEFAULT 0,
	aktiv int(11) NOT NULL DEFAULT 0,
	teilweiseAktiv int(11) NOT NULL DEFAULT 0,
	wenigAktiv int(11) NOT NULL DEFAULT 0,
	inaktiv int(11) NOT NULL DEFAULT 0,
	vacation int(11) NOT NULL DEFAULT 0,
	gesamtanzahl int(11) NOT NULL DEFAULT 0,
	registrierungen int(11) NOT NULL DEFAULT 0,
	maxUserId int(11) NOT NULL DEFAULT 0,
	`version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Statistik der Spieleraktivitaet';
