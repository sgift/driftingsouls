CREATE TABLE `ammo` (
  `id` integer not null auto_increment,
	`areadamage` integer not null,
	`damage` integer not null,
	`destroyable` double precision not null,
	`flags` integer not null,
	`itemid` integer not null,
	`name` varchar(255) not null,
	`picture` varchar(255) not null,
	`shielddamage` integer not null,
	`shotspershot` integer not null,
	`smalltrefferws` integer not null,
	`subdamage` integer not null,
	`subws` integer not null,
	`torptrefferws` integer not null,
	`trefferws` integer not null,
	`type` varchar(255) not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;