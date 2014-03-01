CREATE TABLE `gtu_warenkurse` (
  `place` varchar(255) not null,
	`kurse` longtext not null,
	`name` varchar(255) not null,
  `version` integer not null,
  primary key  (`place`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;