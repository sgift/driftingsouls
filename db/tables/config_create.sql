create table config (
	`name` varchar(255) not null,
	`description` longtext not null,
	`value` longtext not null,
	`version` integer not null,
	primary key (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;