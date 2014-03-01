CREATE TABLE `skn_channels` (
  `id` integer not null auto_increment,
	`allyowner` integer not null,
	`name` varchar(255) not null,
	`readall` boolean not null,
	`readally` integer not null,
	`readnpc` boolean not null,
	`readplayer` longtext not null,
	`version` integer not null,
	`writeall` boolean not null,
  `writeally` integer not null,
	`writenpc` boolean not null,
	`writeplayer` longtext not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
