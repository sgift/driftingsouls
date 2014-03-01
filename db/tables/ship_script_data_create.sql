CREATE TABLE `ship_script_data` (
  `shipid` integer not null auto_increment,
  `script` longtext,
  `scriptexedata` longblob,
  primary key (`shipid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;