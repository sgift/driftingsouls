CREATE TABLE `upgrade_maxvalues` (
  `type` int(11) NOT NULL,
  `maxtiles` int(11) NOT NULL default '1',
  `maxcargo` int(11) NOT NULL default '1',
  PRIMARY KEY  (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
