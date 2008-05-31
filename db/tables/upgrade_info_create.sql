CREATE TABLE `upgrade_info` (
  `id` int(11) NOT NULL default '0',
  `type` int(5) NOT NULL default '1',
  `mod` int(11) NOT NULL default '0',
  `cargo` bool NOT NULL default FALSE,
  `price` int(11) NOT NULL default '0',
  `miningexplosive` int(11) NOT NULL default '0',
  `ore` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
