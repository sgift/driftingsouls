CREATE TABLE `portal_news` (
  `id` integer not null auto_increment,
	`author` longtext not null,
	`date` bigint not null,
	`txt` longtext not null,
	`shortDescription` longtext not null,
	`title` varchar(255) not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;