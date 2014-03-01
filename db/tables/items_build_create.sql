CREATE TABLE `items_build` (
	`id` integer not null auto_increment,
	`buildcosts` varchar(255) not null,
	`buildingid` varchar(255) not null,
	`dauer` decimal(19,5) not null,
	`name` varchar(255) not null,
	`produce` varchar(255) not null,
	`res1` integer not null,
	`res2` integer not null,
	`res3` integer not null,
	primary key  (`id`)
) ENGINE=InnoDB;
