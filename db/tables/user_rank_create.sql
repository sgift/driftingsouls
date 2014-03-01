CREATE TABLE `user_rank` (
  `rank` integer not null,
	`version` integer not null default '0',
	`owner` integer not null,
	`rank_giver` integer not null,
	primary key (`owner`,`rank_giver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;