CREATE TABLE `ships_lost` (
  `id` integer not null auto_increment,
	`ally` integer not null,
	`battle` integer not null,
	`battlelog` varchar(255),
	`destally` integer not null,
	`destowner` integer not null,
  `name` varchar(255),
	`owner` integer not null,
  `tick` integer not null,
	`type` integer not null,
	`version` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;