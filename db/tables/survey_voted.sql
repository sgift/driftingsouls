CREATE TABLE `survey_voted` (
  `survey_id` smallint(5) unsigned NOT NULL default '0',
  `user_id` int(11) NOT NULL default '0',
  KEY `survey_id` (`survey_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE survey_voted ADD CONSTRAINT survey_voted_fk_surveys FOREIGN KEY (survey_id) REFERENCES surveys(id);
ALTER TABLE survey_voted ADD CONSTRAINT survey_voted_fk_users FOREIGN KEY (user_id) REFERENCES users(id);