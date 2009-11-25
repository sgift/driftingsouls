CREATE TABLE `unit_types` (
  			`id` int(11) NOT NULL auto_increment,
  			`name` varchar(50) NOT NULL,
  			`size` tinyint(4) NOT NULL default '1',
  			`description` text,
  			`recost` int(11) NOT NULL default '0',
  			`nahrungcost` int(11) NOT NULL default '1',
  			`kapervalue` int(11) NOT NULL default '1',
  			`buildcosts` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  			`dauer` int(11) NOT NULL default '1',
  			`resid` int(11) NOT NULL default '0',
  			`picture` varchar(50),
  			PRIMARY KEY  (`id`)
		);