CREATE TABLE `user_rank` (
  `rank` integer not null,
	`version` integer not null,
	`owner` integer not null,
	`rank_giver` integer not null,
	primary key (`owner`,`rank_giver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;