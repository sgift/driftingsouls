CREATE TABLE `stats_user_unitcargo` (
  `user_id` int(11) NOT NULL default '0',
  `unitcargo` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die User-UnitCargo-Stats'; 
