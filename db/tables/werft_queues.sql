CREATE TABLE `werft_queues` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `werft` int(11) NOT NULL,
  `position` int(11) NOT NULL,
  `building` int(11) default NULL,
  `item` smallint(6) NOT NULL default '-1',
  `remaining` tinyint(4) NOT NULL default '0',
  `flagschiff` tinyint(1) unsigned NOT NULL default '0',
  `costsPerTick` varchar(300) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `energyPerTick` int(11) NOT NULL default '0',
  `slots` int(11) NOT NULL default '1',
  `scheduled` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `werft_queues_fk_ship_types` (`building`),
  KEY `werft` (`werft`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

ALTER TABLE werft_queues ADD CONSTRAINT werft_queues_fk_werften FOREIGN KEY (werft) REFERENCES werften(id);
ALTER TABLE werft_queues ADD CONSTRAINT werft_queues_fk_ship_types FOREIGN KEY (building) REFERENCES ship_types(id);