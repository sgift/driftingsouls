CREATE TABLE `stats_module_locations` (
  `id` integer not null auto_increment,
	`item_id` integer not null,
	`locations` varchar(255) not null,
	`version` integer not null,
	`user_id` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;