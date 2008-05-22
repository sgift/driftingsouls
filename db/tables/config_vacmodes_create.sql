CREATE TABLE `config_vacmodes` (
  `id` int(11) NOT NULL auto_increment,
  `dauer` int(10) unsigned NOT NULL default '1',
  `vorlauf` int(10) unsigned NOT NULL default '1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
