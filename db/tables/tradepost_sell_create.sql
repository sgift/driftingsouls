CREATE TABLE `tradepost_sell` (
	`resourceid` integer not null,
	`shipid` integer not null,
	`minimum` bigint not null,
	`min_rank` integer not null,
	`price` bigint not null,
	`version` integer not null,
	primary key  (`resourceid`,`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;