CREATE TABLE `gtu_zwischenlager` (
  `id` integer not null auto_increment,
	`cargo1` longtext not null,
	`cargo1need` longtext not null,
	`cargo2` longtext not null,
	`cargo2need` longtext not null,
	`version` integer not null,
	`posten` integer not null,
  `user1` integer not null,
  `user2` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;