CREATE TABLE `config_felsbrocken` (
  `id` integer not null auto_increment,
	`cargo` varchar(255) not null,
	`chance` integer not null,
	`shiptype` integer not null,
  `system` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
