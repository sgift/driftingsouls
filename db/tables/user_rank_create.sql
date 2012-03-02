CREATE TABLE `user_rank` (
  `owner` int(11) NOT NULL,
  `rank_giver` int(11) NOT NULL,
  `rank` int(11) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`owner`,`rank_giver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;