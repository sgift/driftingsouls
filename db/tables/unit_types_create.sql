CREATE TABLE `unit_types` (
	`id` integer NOT NULL AUTO_INCREMENT,
	`name` varchar(50) NOT NULL,
	`size` tinyint(4) NOT NULL DEFAULT '1',
	`description` text,
	`recost` float NOT NULL,
	`nahrungcost` float NOT NULL,
	`kapervalue` integer NOT NULL DEFAULT '1',
	`buildcosts` varchar(120) NOT NULL DEFAULT '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
	`dauer` integer NOT NULL DEFAULT '1',
	`resid` integer NOT NULL DEFAULT '0',
	`picture` varchar(50) DEFAULT NULL,
	`hidden` tinyint(1) DEFAULT '0',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB;