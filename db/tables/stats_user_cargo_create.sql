CREATE TABLE `stats_user_cargo` (
  `user_id` integer NOT NULL default '0',
  `cargo` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
