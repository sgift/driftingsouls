CREATE TABLE `ships_baubar` (
  `id` int(11) NOT NULL auto_increment,
  `type` int(11) NOT NULL default '0',
  `costs` varchar(100) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `crew` smallint(6) NOT NULL default '0',
  `dauer` tinyint(4) NOT NULL default '0',
  `ekosten` int(4) NOT NULL default '0',
  `race` int(11) NOT NULL default '0',
  `systemreq` int(11) NOT NULL default '0',
  `tr1` int(11) NOT NULL default '0',
  `tr2` int(11) NOT NULL default '0',
  `tr3` int(11) NOT NULL default '0',
  `werftslots` int(11) NOT NULL default '1',
  `flagschiff` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
