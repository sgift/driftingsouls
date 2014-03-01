CREATE TABLE `ship_fleets` (
  `id` integer not null auto_increment,
  `name` varchar(255) not null,
  `version` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;