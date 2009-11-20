CREATE TABLE `factory` (
  			`id` int(11) NOT NULL auto_increment,
  			`col` int(11) NOT NULL default '0',
  			`count` tinyint(3) unsigned NOT NULL default '0',
  			`produces` text NOT NULL,
  			`version` int(10) unsigned NOT NULL default '0',
  			`buildingid` int(11) NOT NULL,
  			PRIMARY KEY  (`id`)
		) COMMENT = "Die Fabriken";