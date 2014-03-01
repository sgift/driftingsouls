CREATE TABLE `user_moneytransfer` (
  `id` int(10) unsigned not null auto_increment,
  `from` integer not null default '0',
  `to` integer not null default '0',
  `time` int(10) unsigned not null default '0',
  `count` bigint(20) unsigned not null default '0',
  `text` text not null,
  `fake` tinyint(3) unsigned not null default '0',
  `type` tinyint(3) unsigned not null default '0',
  `version` int(10) unsigned not null default '0',
  primary key  (`id`),
  key `from` (`from`,`to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
