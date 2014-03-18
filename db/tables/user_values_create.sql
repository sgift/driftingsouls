CREATE TABLE `user_values` (
  `id` integer not null auto_increment,
  `name` varchar(255) not null,
  `value` longtext not null,
  `version` integer not null,
	`user_id` integer not null,
	primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
