CREATE TABLE `skn_visits` (
  `id` integer not null auto_increment,
	`time` bigint not null,
	`version` integer not null,
	`channel` integer not null,
	`user` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;