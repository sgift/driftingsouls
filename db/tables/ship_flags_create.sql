CREATE TABLE `ship_flags` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `flagType` int(11) NOT NULL,
  `ship` int(11) NOT NULL,
  `remaining` int(11) NOT NULL,
  `version` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin AUTO_INCREMENT=1;