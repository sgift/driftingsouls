CREATE TABLE `tradepost_buy_limit` (
	`resourceid` integer not null,
	`shipid` integer not null,
  `maximum` bigint not null,
  `min_rank` integer not null,
  `version` integer not null,
  primary key  (`resourceid`,`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;