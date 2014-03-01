CREATE TABLE `orders_offiziere` (
  `id` integer not null auto_increment,
	`com` integer not null,
	`cost` integer not null,
	`ing` integer not null,
	`name` varchar(255) not null,
	`nav` integer not null,
	`rang` integer not null,
	`sec` integer not null,
	`waf` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;