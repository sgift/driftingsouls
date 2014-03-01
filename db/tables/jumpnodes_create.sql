CREATE TABLE `jumpnodes` (
  `id` integer not null auto_increment,
	`gcpcolonistblock` boolean not null,
	`hidden` integer not null,
	`name` varchar(255) not null,
	`system` integer not null,
	`systemout` integer not null,
	`wpnblock` boolean not null,
	`x` integer not null,
	`xout` integer not null,
	`y` integer not null,
  `yout` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
