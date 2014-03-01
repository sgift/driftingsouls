CREATE TABLE `logging` (
  `id` bigint not null auto_increment,
	`data` longtext,
	`source` varchar(255) not null,
	`target` varchar(255) not null,
	`time` bigint not null,
	`type` varchar(255) not null,
	`user_id` integer not null,
  `version` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;