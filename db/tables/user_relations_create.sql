CREATE TABLE `user_relations` (
  `id` integer not null auto_increment,
	`status` integer not null,
	`version` integer not null,
	`target_id` integer not null,
	`user_id` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;