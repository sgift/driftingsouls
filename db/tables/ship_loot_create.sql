CREATE TABLE `ship_loot` (
  `id` integer not null auto_increment,
	`chance` integer not null,
	`count` integer not null,
	`resource` varchar(255) not null,
	`shiptype` integer not null,
	`totalmax` integer not null,
	`version` integer not null,
	`owner` integer not null,
  `targetuser` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;