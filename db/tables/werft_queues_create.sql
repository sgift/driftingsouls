CREATE TABLE `werft_queues` (
  `id` integer not null auto_increment,
	`flagschiff` boolean not null,
	`item` integer not null,
	`costsPerTick` longtext not null,
	`energyPerTick` integer not null,
	`position` integer not null,
	`remaining` integer not null,
	`scheduled` boolean not null,
	`slots` integer not null,
	`building` integer not null,
	`werft` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;