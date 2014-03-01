CREATE TABLE `upgrade_job` (
  `id` integer NOT NULL auto_increment,
  `baseid` integer NOT NULL default '0',
  `userid` integer NOT NULL default '0',
  `tiles` integer NOT NULL default '0',
  `cargo` integer NOT NULL default '0',
  `bar` bool NOT NULL default FALSE,
  `payed` bool NOT NULL default FALSE,
  `colonizerid` integer,
  `end` integer NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
