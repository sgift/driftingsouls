CREATE TABLE `orders` (
  ordertype varchar(31) not null,
  `id` integer not null auto_increment,
  `tick` integer not null,
  `user` integer not null,
  `version` integer not null,
	`type` integer,
	flags varchar(255),
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;