CREATE TABLE `tradepost_buy_limit` (
	id bigint not null auto_increment,
	`maximum` bigint not null,
  `min_rank` integer not null,
	`resourceid` integer not null,
	`version` integer not null,
	`shipid` integer not null,
	primary key (id),
	unique (shipid, resourceid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;