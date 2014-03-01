CREATE TABLE `jumps` (
  `id` integer not null auto_increment,
	`system` integer not null,
	`version` integer not null,
	`x` integer not null,
	`y` integer not null,
	`shipid` integer not null,
  primary key (`id`),
	unique (shipid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;