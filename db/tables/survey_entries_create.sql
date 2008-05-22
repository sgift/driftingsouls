CREATE TABLE `survey_entries` (
  `id` int(11) NOT NULL auto_increment,
  `survey_id` smallint(5) unsigned NOT NULL default '0',
  `name` text NOT NULL,
  `type` varchar(15) NOT NULL default '',
  `params` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `surveys_id` (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
