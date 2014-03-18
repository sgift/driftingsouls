CREATE TABLE `tasks` (
  `taskid` varchar(255) not null,
	`data1` varchar(255),
	`data2` varchar(255),
	`data3` varchar(255),
	`time` bigint not null,
	`timeout` integer not null,
	`type` integer not null,
  `version` integer not null,
  primary key  (`taskid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;