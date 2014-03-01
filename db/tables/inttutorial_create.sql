CREATE TABLE `inttutorial` (
  `id` integer not null auto_increment,
	`headimg` varchar(255) not null,
	`reqbase` integer not null,
	`reqname` integer not null,
	`reqsheet` integer not null,
	`reqship` integer not null,
  `text` longtext not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;