CREATE TABLE `stats_user_cargo` (
  `user_id` integer not null,
  `cargo` longtext not null,
  `version` integer not null,
  primary key  (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;