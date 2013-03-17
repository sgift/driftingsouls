CREATE TABLE `base_types` (
		`id` int(11) NOT NULL auto_increment,
		`Name` varchar(30) NOT NULL,
		`width` int(11) NOT NULL,
		`height` int(11) NOT NULL,
		`maxtiles` int(11) NOT NULL,
		`cargo` int(11) NOT NULL,
		`energy` int(11) NOT NULL,
		`terrain` text,
		`spawnableress` text,
		`size` int(11) not null,
		PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;