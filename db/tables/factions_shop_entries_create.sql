CREATE TABLE `factions_shop_entries` (
  `id` integer not null auto_increment,
	`availability` integer not null,
	`faction_id` integer not null,
	lpKosten bigint not null,
	`min_rank` integer not null,
	`price` bigint not null,
	`resource` varchar(255) not null,
  `type` integer not null,
  `version` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;