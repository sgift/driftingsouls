CREATE TABLE `sectors` (
  `system` integer not null,
  `x` integer not null,
  `y` integer not null,
  `objects` integer not null,
  `onenter` longtext not null,
  `version` integer not null,
  primary key  (`system`,`x`,`y`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;