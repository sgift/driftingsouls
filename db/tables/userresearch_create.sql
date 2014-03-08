CREATE TABLE `userresearch` (
	`id` integer not null auto_increment,
	`owner` integer not null,
	`research` integer not null,
	primary key (`id`),
	unique (`owner`,`research`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;