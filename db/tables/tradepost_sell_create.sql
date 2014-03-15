CREATE TABLE `tradepost_sell` (
	id bigint not null auto_increment,
	`minimum` bigint not null,
	`min_rank` integer not null,
	`price` bigint not null,
	`resourceid` integer not null,
	`version` integer not null,
	`shipid` integer not null,
	primary key  (`id`),
	unique (shipid, resourceid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;