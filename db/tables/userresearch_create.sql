CREATE TABLE `userresearch` (
	`id` int(11) unsigned NOT NULL auto_increment,
	`owner` int(11) NOT NULL,
	`research` int(11) NOT NULL,
	PRIMARY KEY  (`id`),
	UNIQUE (`owner`,`research`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Forschungen der Spieler'; 