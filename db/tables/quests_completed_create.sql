CREATE TABLE `quests_completed` (
  `id` integer not null auto_increment,
  `questid` integer not null,
  `userid` integer not null,
  primary key  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

