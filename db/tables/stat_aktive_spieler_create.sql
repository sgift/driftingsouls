CREATE TABLE `stat_aktive_spieler` (
  `tick` integer not null,
	aktiv integer not null,
	gesamtanzahl integer not null,
	inaktiv integer not null,
	maxUserId integer not null,
	registrierungen integer not null,
	sehrAktiv integer not null,
	teilweiseAktiv integer not null,
	vacation integer not null,
	`version` integer not null,
	wenigAktiv integer not null,
  primary key  (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;