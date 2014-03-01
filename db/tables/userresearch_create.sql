CREATE TABLE `userresearch` (
	`id` int(11) unsigned NOT NULL auto_increment,
	`owner` integer NOT NULL,
	`research` integer NOT NULL,
	PRIMARY KEY  (`id`),
	UNIQUE (`owner`,`research`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Forschungen der Spieler'; 