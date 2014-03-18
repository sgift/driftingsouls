CREATE TABLE `upgrade_job` (
  `id` integer not null auto_increment,
	`bar` boolean not null,
	`end` integer not null,
	`payed` boolean not null,
	`baseid` integer not null,
	`cargo` integer not null,
	`colonizerid` integer,
	`tiles` integer not null,
	`userid` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;