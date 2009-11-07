CREATE TABLE `cores` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(50) NOT NULL default 'Noname',
  `astitype` tinyint(3) unsigned NOT NULL default '1',
  `buildcosts` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `consumes` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `produces` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `arbeiter` int(11) NOT NULL default '0',
  `ever` int(11) NOT NULL default '0',
  `eprodu` int(11) NOT NULL default '0',
  `bewohner` int(11) NOT NULL default '0',
  `techreq` int(11) NOT NULL default '0',
  `eps` int(11) NOT NULL default '0',
  `shutdown` tinyint (1) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
