CREATE TABLE `stats_verkaeufe` (
  `id` integer not null auto_increment,
	`place` varchar(255) not null,
	`stats` longtext not null,
	`system` integer not null,
	`tick` integer not null,
  `version` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;