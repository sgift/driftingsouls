CREATE TABLE `global_sectortemplates` (
  `id` varchar(30) NOT NULL default '',
  `x` smallint(5) unsigned NOT NULL default '0',
  `y` smallint(5) unsigned NOT NULL default '0',
  `w` smallint(5) unsigned NOT NULL default '0',
  `h` smallint(5) unsigned NOT NULL default '0',
  `scriptid` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Sectortemplatemanager-IDs'; 

INSERT INTO `global_sectortemplates` (`id`, `x`, `y`, `w`, `h`, `scriptid`) VALUES ('ORDER_TERRANER', 1, 1, 0, 0, 0);
INSERT INTO `global_sectortemplates` (`id`, `x`, `y`, `w`, `h`, `scriptid`) VALUES ('ORDER_TERRANER_TANKER', 1, 2, 0, 0, 0);
INSERT INTO `global_sectortemplates` (`id`, `x`, `y`, `w`, `h`, `scriptid`) VALUES ('ORDER_VASUDANER', 2, 1, 0, 0, 0);
INSERT INTO `global_sectortemplates` (`id`, `x`, `y`, `w`, `h`, `scriptid`) VALUES ('ORDER_VASUDANER_TANKER', 2, 2, 0, 0, 0);
