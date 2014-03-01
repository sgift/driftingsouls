CREATE TABLE `quests_running` (
  `id` integer not null auto_increment,
	`execdata` longblob not null,
	`ontick` integer,
	`publish` integer not null,
	`statustext` varchar(255) not null,
	`uninstall` longtext,
	`questid` integer not null,
  `userid` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;