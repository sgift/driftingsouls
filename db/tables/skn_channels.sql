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

INSERT INTO `skn_channels` (`id`, `name`, `allyowner`, `writeall`, `readall`, `writenpc`, `readnpc`, `writeally`, `readally`, `writeplayer`, `readplayer`) VALUES (1, 'Standard', 0, 1, 1, 0, 0, 0, 0, '', '');
INSERT INTO `skn_channels` (`id`, `name`, `allyowner`, `writeall`, `readall`, `writenpc`, `readnpc`, `writeally`, `readally`, `writeplayer`, `readplayer`) VALUES (2, 'Notfrequenz', 0, 1, 1, 0, 0, 0, 0, '', '');
INSERT INTO `skn_channels` (`id`, `name`, `allyowner`, `writeall`, `readall`, `writenpc`, `readnpc`, `writeally`, `readally`, `writeplayer`, `readplayer`) VALUES (3, 'GNN News Network', 0, 0, 1, 1, 0, 0, 0, '', '');
INSERT INTO `skn_channels` (`id`, `name`, `allyowner`, `writeall`, `readall`, `writenpc`, `readnpc`, `writeally`, `readally`, `writeplayer`, `readplayer`) VALUES (4, 'Non-RPG', 0, 1, 1, 0, 0, 0, 0, '', '');
INSERT INTO `skn_channels` (`id`, `name`, `allyowner`, `writeall`, `readall`, `writenpc`, `readnpc`, `writeally`, `readally`, `writeplayer`, `readplayer`) VALUES (5, '-  TESTER  CN -', 1, 1, 1, 0, 0, 0, 0, '', '');