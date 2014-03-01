CREATE TABLE `sessions` (
  `id` integer not null auto_increment,
	`tick` bigint not null,
	`token` varchar(255) not null,
	`userid` integer not null,
  `version` integer not null,
  primary key(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;