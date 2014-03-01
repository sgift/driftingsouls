CREATE TABLE `global_sectortemplates` (
  `id` varchar(255) not null,
	`h` integer not null,
	`scriptid` integer not null,
	`w` integer not null,
	`x` integer not null,
  `y` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;