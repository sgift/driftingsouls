CREATE TABLE `stats_user_cargo` (
	id bigint not null auto_increment,
	`cargo` longtext not null,
  `version` integer not null,
	`user_id` integer not null,
	primary key  (`id`),
	unique (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;