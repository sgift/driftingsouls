CREATE TABLE `survey_results` (
  `id` int(11) NOT NULL auto_increment,
  `survey_id` smallint(5) unsigned NOT NULL default '0',
  `result` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `surveys_id` (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
