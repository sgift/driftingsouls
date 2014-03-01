CREATE TABLE `kaserne_queues` (
	`id` integer not null auto_increment,
	`count` integer not null,
	`remaining` integer not null,
	`kaserne` integer not null,
	`unitid` integer not null,
	primary key(`id`)
) ENGINE=InnoDB;