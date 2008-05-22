CREATE TABLE `ship_fleets` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(90) NOT NULL default '',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Flottenverbaende'; 
