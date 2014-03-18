CREATE TABLE `upgrade_info` (
  `id` integer not null auto_increment,
	`cargo` boolean not null,
	`miningexplosive` integer not null,
	`mod` integer not null,
	`ore` integer not null,
	`price` integer not null,
	`type` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;