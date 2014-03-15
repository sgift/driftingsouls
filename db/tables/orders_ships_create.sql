CREATE TABLE `orders_ships` (
	id integer not null auto_increment,
	`cost` integer not null,
  rasse integer not null,
	`type` integer not null,
	primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
