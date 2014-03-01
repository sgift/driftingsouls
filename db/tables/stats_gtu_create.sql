CREATE TABLE `stats_gtu` (
  `id` integer not null auto_increment,
	`gtugew` double precision not null,
	`mtype` integer not null,
	`owner` integer not null,
	`ownername` varchar(255) not null,
	`preis` bigint not null,
	`type` longtext not null,
	`userid` integer not null,
	`username` varchar(255) not null,
  `version` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;