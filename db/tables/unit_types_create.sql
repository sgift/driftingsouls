CREATE TABLE `unit_types` (
	`id` integer not null auto_increment,
	`buildcosts` varchar(255) not null,
	`dauer` integer not null,
	`description` longtext,
	`hidden` boolean not null,
	`kapervalue` integer not null,
	`nahrungcost` double precision not null,
	`name` varchar(255) not null,
	`picture` varchar(255) not null,
	`recost` double precision not null,
	`resid` integer not null,
	`size` integer not null,
	primary key (`id`)
) ENGINE=InnoDB;