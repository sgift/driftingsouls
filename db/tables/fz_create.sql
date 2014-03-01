CREATE TABLE `fz` (
  `id` integer not null auto_increment,
	`dauer` integer not null,
	`type` integer not null,
	`version` integer not null,
	`forschung` integer,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
