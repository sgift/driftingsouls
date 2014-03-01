CREATE TABLE `stats_ships` (
  `tick` integer not null,
	`crewcount` bigint not null,
	`shipcount` bigint not null,
  `version` integer not null,
  primary key (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;