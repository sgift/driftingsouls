CREATE TABLE `systems` (
  			`id` tinyint(4) auto_increment,
  			`Name` varchar(30) NOT NULL,
  			`width` smallint(6) NOT NULL,
  			`height` smallint(6) NOT NULL,
  			`military` tinyint(1) NOT NULL,
  			`maxColonies` tinyint(3) NOT NULL default '-1',
  			`starmap` tinyint(1) NOT NULL default '0',
  			`orderloc` text,
  			`gtuDropZone` varchar(10) default NULL,
  			`access` tinyint(2) NOT NULL,
  			`descrip` text,
  			spawnableress text,
  			PRIMARY KEY  (`id`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8;