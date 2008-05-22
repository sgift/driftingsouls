CREATE TABLE `skn_channels` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL default '',
  `allyowner` int(11) NOT NULL default '0',
  `writeall` tinyint(1) NOT NULL default '1',
  `readall` tinyint(1) NOT NULL default '1',
  `writenpc` tinyint(1) NOT NULL default '0',
  `readnpc` tinyint(1) NOT NULL default '0',
  `writeally` int(11) NOT NULL default '0',
  `readally` int(11) NOT NULL default '0',
  `readplayer` text NOT NULL,
  `writeplayer` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `allyowner` (`allyowner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Com-Net Frequenzen'; 
