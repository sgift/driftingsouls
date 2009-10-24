CREATE TABLE `forschungen` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(60) NOT NULL default '',
  `req1` int(11) NOT NULL default '0',
  `req2` int(11) NOT NULL default '0',
  `req3` int(11) NOT NULL default '0',
  `time` int(11) NOT NULL default '0',
  `costs` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `descrip` text NOT NULL,
  `race` tinyint(4) NOT NULL default '0',
  `visibility` tinyint(4) NOT NULL default '1',
  `flags` varchar(60) NOT NULL default '',
  `specializationCosts` int NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
