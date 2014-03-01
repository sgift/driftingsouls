CREATE TABLE `stats_cargo` (
  `tick` integer not null,
  `cargo` longtext not null,
  `version` integer not null,
  primary key (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;