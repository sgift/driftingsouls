CREATE TABLE `handel` (
  `id` integer not null auto_increment,
	`bietet` longtext not null,
	`comm` longtext not null,
	`sucht` longtext not null,
	`time` bigint not null,
	`version` integer not null,
	`who` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;