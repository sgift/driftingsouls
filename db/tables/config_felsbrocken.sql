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

INSERT INTO `config_felsbrocken` (`id`, `shiptype`, `system`, `chance`, `cargo`) VALUES (1, 77, 7, 55, '0,0,0,0,0,0,0,0,0,4,0,0,0,0,7,0,0,0,'),
(2, 77, 7, 28, '0,0,0,3,0,0,0,0,0,0,0,0,0,0,3,0,0,0,'),
(3, 77, 7, 15, '0,0,0,0,0,0,0,6,0,0,0,0,0,0,20,0,0,0,'),
(4, 77, 7, 2, '0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,1,0,0,');

ALTER TABLE `config_felsbrocken`
  ADD CONSTRAINT `fk_config_felsbrocken_system` FOREIGN KEY (`system`) REFERENCES `config_felsbrocken_systems` (`system`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_config_felsbrocken_shiptype` FOREIGN KEY (`shiptype`) REFERENCES `ship_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;