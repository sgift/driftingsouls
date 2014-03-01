CREATE TABLE `werft_queues` (
  `id` int(10) unsigned not null auto_increment,
  `werft` integer not null,
  `position` integer not null,
  `building` integer default null,
  `item` smallint(6) not null default '-1',
  `remaining` tinyint(4) not null default '0',
  `flagschiff` tinyint(1) unsigned not null default '0',
  `costsPerTick` varchar(300) not null default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `energyPerTick` integer not null default '0',
  `slots` integer not null default '1',
  `scheduled` tinyint(1) not null default '0',
  primary key  (`id`),
  key `werft_queues_fk_ship_types` (`building`),
  key `werft` (`werft`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
