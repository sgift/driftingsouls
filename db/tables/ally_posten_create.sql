CREATE TABLE `ally_posten` (
  `id` integer not null auto_increment,
  `name` varchar(255) not null,
  `version` integer not null,
	`ally` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;