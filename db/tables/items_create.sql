CREATE TABLE `items` (
  			`id` int(11) NOT NULL,
  			`name` varchar(100) NOT NULL,
  			`picture` text,
  			`largepicture` text,
  			`cargo` int(5) NOT NULL default '1',
  			`effect` text,
  			`quality` varchar(15) default 'common',
  			`description` text,
  			`handel` tinyint(1) NOT NULL,
  			`accesslevel` int(11) NOT NULL,
  			`unknownitem` tinyint(1) NOT NULL,
  			`isspawnable` tinyint (1) NOT NULL,
  			PRIMARY KEY  (`id`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8;