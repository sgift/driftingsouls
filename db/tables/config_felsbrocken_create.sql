CREATE TABLE `config_felsbrocken` (
  `id` int(11) NOT NULL auto_increment,
  `shiptype` int(11) NOT NULL default '0',
  `system` tinyint(4) NOT NULL default '1',
  `chance` tinyint(4) NOT NULL default '1',
  `cargo` varchar(255) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  PRIMARY KEY  (`id`),
  KEY `system` (`system`),
  KEY `fk_config_felsbrocken_shiptype` (`shiptype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die verschiedenen Felsbrockenbestueckungen';
