CREATE TABLE `base_types` (
	`id` integer not null auto_increment,
	`cargo` integer not null,
	`energy` integer not null,
	`height` integer not null,
	`maxtiles` integer not null,
	`name` varchar(255) not null,
	`size` integer not null,
	`spawnableress` longtext,
	`terrain` longtext,
	`width` integer not null,
	primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
