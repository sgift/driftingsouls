CREATE TABLE schiff_einstellungen (
	`id` integer not null auto_increment,
	`autodeut` tinyint not null,
	`bookmark` boolean not null,
	`destcom` longtext not null,
	`destsystem` integer not null,
	`destx` integer not null,
	`desty` integer not null,
	`isallyfeeding` boolean not null,
	`isfeeding` boolean not null,
	`showtradepost` integer not null,
	`startFighters` boolean not null,
	`version` integer not null,
	primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;