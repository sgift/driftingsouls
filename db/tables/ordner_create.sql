CREATE TABLE `ordner` (
  `id` integer not null auto_increment,
	`flags` integer not null,
	`name` varchar(255) not null,
	`parent` integer not null,
	`version` integer not null,
	`playerid` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;