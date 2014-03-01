CREATE TABLE `academy_queue_entry` (
	`id` integer not null auto_increment,
	`position` integer not null,
	`remaining` integer not null,
	`scheduled` boolean not null,
	`training` integer not null,
	`trainingtype` integer not null,
	`academy_id` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;