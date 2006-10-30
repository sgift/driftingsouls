CREATE TABLE `portal_news` (
  `id` mediumint(9) NOT NULL auto_increment,
  `title` varchar(50) NOT NULL default '',
  `author` varchar(80) NOT NULL default '',
  `date` int(11) NOT NULL default '0',
  `txt` text NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

INSERT INTO `portal_news` (`id`, `title`, `author`, `date`, `txt`) VALUES (1, 'Willkommen bei DS2', 'bKtHeG', 1162242585, 'Willkommen bei DS2\r\nDu hasst offenbar den Demodatensatz erfolgreich installiert.\r\nViel Spass beim Testen und Patchen!');