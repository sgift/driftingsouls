CREATE TABLE `factions_shop_orders` (
  `id` integer not null auto_increment,
	`adddata` longtext,
	`count` integer not null,
	`date` bigint not null,
	lpKosten bigint not null,
	`price` bigint not null,
	`status` integer not null,
	`version` integer not null,
	`shopentry_id` integer not null,
  `user_id` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;