CREATE TABLE `cores` (
  `id` integer not null auto_increment,
	`arbeiter` integer not null,
	`astitype` integer not null,
	`bewohner` integer not null,
	`buildcosts` varchar(255) not null,
  `consumes` varchar(255) not null,
	`eprodu` integer not null,
	`ever` integer not null,
	`eps` integer not null,
	`name` varchar(255) not null,
	`produces` varchar(255) not null,
	`shutdown` boolean not null,
	`techreq` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;