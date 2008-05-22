CREATE TABLE `portal_news` (
  `id` mediumint(9) NOT NULL auto_increment,
  `title` varchar(50) NOT NULL default '',
  `author` varchar(80) NOT NULL default '',
  `date` int(11) NOT NULL default '0',
  `txt` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
