CREATE TABLE `upgrade_info` (
  `id` integer NOT NULL default '0',
  `type` int(5) NOT NULL default '1',
  `mod` integer NOT NULL default '0',
  `cargo` bool NOT NULL default FALSE,
  `price` integer NOT NULL default '0',
  `miningexplosive` integer NOT NULL default '0',
  `ore` integer NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
