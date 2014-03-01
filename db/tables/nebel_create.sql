CREATE TABLE `nebel` (
	`system` integer not null,
	`x` integer not null,
  `y` integer not null,
  `type` integer not null,
  primary key (`system`,`x`,`y`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
