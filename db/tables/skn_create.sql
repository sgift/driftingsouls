CREATE TABLE `skn` (
  `post` integer not null auto_increment,
	`allypic` integer not null,
	`head` varchar(255) not null,
	`name` varchar(255) not null,
	`pic` integer not null,
	`text` longtext not null,
	`tick` integer not null,
	`time` bigint not null,
	`version` integer not null,
	`channel` integer not null,
	`userid` integer not null,
	primary key (`post`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;