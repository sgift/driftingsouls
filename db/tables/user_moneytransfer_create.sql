CREATE TABLE `user_moneytransfer` (
  `id` integer not null auto_increment,
	`count` decimal(19,2) not null,
	`fake` integer not null,
	`text` longtext not null,
	`time` bigint not null,
	`type` integer not null,
	`version` integer not null,
	`from_id` integer not null,
  `to_id` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;