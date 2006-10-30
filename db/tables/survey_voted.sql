CREATE TABLE `survey_voted` (
  `survey_id` smallint(5) unsigned NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  KEY `survey_id` (`survey_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
