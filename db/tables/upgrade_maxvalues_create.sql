CREATE TABLE `upgrade_maxvalues` (
  `type` integer not null,
	`maxcargo` integer not null,
	`maxtiles` integer not null,
  primary key (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;