CREATE TABLE `jumpnodes` (
  `id` int(11) NOT NULL auto_increment,
  `x` int(11) NOT NULL default '0',
  `y` int(11) NOT NULL default '0',
  `system` int(11) NOT NULL default '0',
  `xout` int(11) NOT NULL default '0',
  `yout` int(11) NOT NULL default '0',
  `systemout` int(11) NOT NULL default '0',
  `name` varchar(40) NOT NULL default '',
  `wpnblock` tinyint(1) NOT NULL default '0',
  `gcpcolonistblock` tinyint(1) NOT NULL default '0',
  `hidden` tinyint(3) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `coords` (`x`,`y`,`system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
