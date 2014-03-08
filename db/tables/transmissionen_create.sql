CREATE TABLE `transmissionen` (
  `id` integer not null auto_increment,
	`flags` integer not null,
	`gelesen` integer not null,
	`inhalt` longtext not null,
	`kommentar` longtext not null,
	`ordner` integer not null,
	`time` bigint not null,
	`title` varchar(255) not null,
	`version` integer not null,
	`empfaenger` integer not null,
	`sender` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;