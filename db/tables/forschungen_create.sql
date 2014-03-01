CREATE TABLE `forschungen` (
  `id` integer not null auto_increment,
	`costs` varchar(255) not null,
	`description` longtext not null,
	`flags` varchar(255) not null,
	image varchar(255),
	`name` varchar(255) not null,
	`race` integer not null,
	`req1` integer not null,
  `req2` integer not null,
  `req3` integer not null,
	`specializationCosts` integer not null,
	`time` integer not null,
  `visibility` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
