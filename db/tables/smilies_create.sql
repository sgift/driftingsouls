CREATE TABLE `smilies` (
  `id` integer not null auto_increment,
	`image` varchar(255) not null,
	`tag` varchar(255) not null,
  `version` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;