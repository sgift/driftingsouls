CREATE TABLE `scripts` (
  `id` integer not null auto_increment,
  `name` varchar(255) not null,
  `script` longtext not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;