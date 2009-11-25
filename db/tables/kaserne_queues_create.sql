CREATE TABLE `kaserne_queues` (
			`id` int(11) unsigned NOT NULL auto_increment,
			`kaserne` int(11) unsigned NOT NULL,
			`unitid` int(11) unsigned NOT NULL,
			`count` int(11) unsigned NOT NULL,
			`remaining` int(11) unsigned NOT NULL,
			PRIMARY KEY(`id`)
		);