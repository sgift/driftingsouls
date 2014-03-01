CREATE TABLE `battles_ships` (
  `shipid` integer not null,
	`ablativeArmor` integer not null,
	`action` integer not null,
	`comm` integer not null,
	`destroyer` integer not null,
	`engine` integer not null,
	`hull` integer not null,
	`sensors` integer not null,
	`shields` integer not null,
	`side` integer not null,
	`version` integer not null,
	`weapons` integer not null,
	`battleid` integer not null,
  primary key (`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;