CREATE TABLE `academy_queues` (
  `id` int(11) NOT NULL auto_increment,
  `academy_id` int(11) NOT NULL,
  `training` int(11) default NULL,
  `trainingtype` smallint(6) default NULL,
  `remaining` smallint(6) default NULL,
  `scheduled` tinyint(1) default NULL,
  `position` int(6) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;