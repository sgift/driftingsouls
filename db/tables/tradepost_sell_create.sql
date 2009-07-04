CREATE TABLE `tradepost_sell` (
	`shipid` int(11) NOT NULL,
	`resourceid` int(11) NOT NULL,
	`price` int(11) NOT NULL,
	`minimum` int(11) NOT NULL,
	`version` int(10) unsigned not null default '0',
	PRIMARY KEY  (`shipid`,`resourceid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
