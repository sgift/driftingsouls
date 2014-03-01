CREATE TABLE `tradepost_sell` (
	`shipid` integer NOT NULL,
	`resourceid` integer NOT NULL,
	`price` integer NOT NULL,
	`minimum` integer NOT NULL,
	`min_rank` integer NOT NULL DEFAULT  '0',
	`version` int(10) unsigned not null default '0',
	PRIMARY KEY  (`shipid`,`resourceid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
