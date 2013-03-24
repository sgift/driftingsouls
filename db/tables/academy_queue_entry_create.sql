CREATE TABLE `academy_queue_entry` (
  `id` int(11) NOT NULL auto_increment,
  `academy_id` int(11) NOT NULL,
  `training` int(11) default NULL,
  `trainingtype` smallint(6) default NULL,
  `remaining` smallint(6) default NULL,
  `scheduled` tinyint(1) default NULL,
  `position` int(6) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;