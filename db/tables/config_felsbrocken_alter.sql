ALTER TABLE `config_felsbrocken`
  ADD CONSTRAINT `fk_config_felsbrocken_system` FOREIGN KEY (`system`) REFERENCES `config_felsbrocken_systems` (`system`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_config_felsbrocken_shiptype` FOREIGN KEY (`shiptype`) REFERENCES `ship_types` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;
