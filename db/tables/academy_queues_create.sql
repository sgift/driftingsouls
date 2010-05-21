CREATE TABLE `academy_queues` (
  `id` int(11) NOT NULL auto_increment,
  `base` int(11) default NULL,
  `training` int(11) default NULL,
  `trainingtype` smallint(6) default NULL,
  `remaining` smallint(6) default NULL,
  `scheduled` tinyint(1) default NULL,
  `position` int(6) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=22 DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;