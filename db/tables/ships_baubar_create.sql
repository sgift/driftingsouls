CREATE TABLE `ships_baubar` (
  `id` integer not null auto_increment,
	`costs` longtext not null,
	`crew` integer not null,
  `dauer` integer not null,
  `ekosten` integer not null,
	`flagschiff` boolean not null,
  `race` integer not null,
  `tr1` integer not null,
  `tr2` integer not null,
  `tr3` integer not null,
	`werftslots` integer not null,
	`type` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;