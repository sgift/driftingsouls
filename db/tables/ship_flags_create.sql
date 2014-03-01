CREATE TABLE `ship_flags` (
  `id` integer not null auto_increment,
  `flagType` integer not null,
	`remaining` integer not null,
	`version` integer not null,
	`ship` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;