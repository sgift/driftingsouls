CREATE TABLE `ship_script_data` (
  `shipid` int(11) NOT NULL,
  `script` text,
  `scriptexedata` blob,
  PRIMARY KEY (`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;