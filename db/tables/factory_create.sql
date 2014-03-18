CREATE TABLE `factory` (
	`id` integer not null auto_increment,
	`buildingid` integer not null,
	`count` integer not null,
	`produces` longtext not null,
	`version` integer not null,
	`col` integer,
	primary key  (`id`),
	unique (col, buildingid)
) ENGINE=InnoDB;
