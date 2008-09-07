CREATE TABLE `upgrade_job` (
  `id` int(11) NOT NULL auto_increment,
  `baseid` int(11) NOT NULL default '0',
  `userid` int(11) NOT NULL default '0',
  `tiles` int(11) NOT NULL default '0',
  `cargo` int(11) NOT NULL default '0',
  `bar` bool NOT NULL default FALSE,
  `payed` bool NOT NULL default FALSE,
  `colonizerid` int(11),
  `end` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
