CREATE TABLE `quests_text` (
  `id` integer not null auto_increment,
	`comment` varchar(255) not null,
	`picture` varchar(255) not null,
	`text` longtext not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;