CREATE TABLE `werften` (
	`werfttype` char(1) not null,
	`id` integer not null auto_increment,
	`flagschiff` boolean not null,
	`type` integer not null,
	`version` integer not null,
	`komplex` boolean,
	`linkedWerft` integer,
	`linked` integer,
	`shipid` integer,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;