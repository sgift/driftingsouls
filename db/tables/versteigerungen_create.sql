CREATE TABLE `versteigerungen` (
	`mtype` integer not null,
	`id` integer not null auto_increment,
  `preis` bigint not null,
	`tick` integer not null,
	`version` integer not null,
	`type` varchar(255),
  `bieter` integer not null,
  `owner` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;