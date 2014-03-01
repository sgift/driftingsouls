CREATE TABLE `upgrade_maxvalues` (
  `type` integer NOT NULL,
  `maxtiles` integer NOT NULL default '1',
  `maxcargo` integer NOT NULL default '1',
  PRIMARY KEY  (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
