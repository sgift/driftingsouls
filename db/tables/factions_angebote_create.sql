CREATE TABLE `factions_angebote` (
  `id` integer not null auto_increment,
	`description` longtext not null,
	`faction` integer not null,
	`image` varchar(255) not null,
	`title` varchar(255) not null,
  `version` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;