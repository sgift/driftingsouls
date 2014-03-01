CREATE TABLE `items` (
	`id` integer not null,
	`accesslevel` integer not null,
	`cargo` bigint not null,
	`description` longtext,
	`effect` longtext,
	`handel` boolean not null,
	`isspawnable` boolean not null,
	`largepicture` longtext,
	`name` varchar(255) not null,
	`picture` longtext,
	`quality` varchar(255),
	`unknownitem` boolean not null,
	primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;