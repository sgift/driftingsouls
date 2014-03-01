CREATE TABLE `cargo_entries_units` (
	`type` integer not null,
	`id` bigint not null auto_increment,
	`amount` bigint not null,
	`unittype` integer not null,
	`basis_id` integer,
	`schiff_id` integer,
	primary key (`id`),
	unique (`type`,`basis_id`,`schiff_id`,`unittype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;