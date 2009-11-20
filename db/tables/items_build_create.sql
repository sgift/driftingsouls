CREATE TABLE `items_build` (
  			`id` int(11) NOT NULL auto_increment,
			`name` varchar(50) NOT NULL,
			`res1` int(11) NOT NULL default '0',
			`res2` int(11) NOT NULL default '0',
			`res3` int(11) NOT NULL default '0',
			`dauer` float NOT NULL default '0.01',
			`buildcosts` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
			`buildingid` int (11) NOT NULL,
			`produce` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
			PRIMARY KEY  (`id`)
		);