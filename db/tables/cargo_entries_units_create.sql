CREATE TABLE `cargo_entries_units` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`type` int(11) NOT NULL,
	`unittype` int(11) NOT NULL,
	`amount` int(11) NOT NULL,
	`basis_id` int(11) DEFAULT NULL,
	`schiff_id` int(11) DEFAULT NULL,
	PRIMARY KEY (`id`),
	UNIQUE KEY `type` (`type`,`basis_id`,`schiff_id`,`unittype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;