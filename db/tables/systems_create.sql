CREATE TABLE `systems` (
	`id` integer not null auto_increment,
	`military` boolean not null,
	`descrip` longtext,
	`gtuDropZone` varchar(255),
	`height` integer not null,
	`starmap` boolean not null,
	mapX integer not null,
	mapY integer not null,
	`maxColonies` integer not null,
	`Name` varchar(255) not null,
	`orderloc` longtext,
	spawnableress longtext,
	`access` integer not null,
	`width` integer not null,
	primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;