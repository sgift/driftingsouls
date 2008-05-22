CREATE TABLE `config` (
  `ticks` int(11) NOT NULL default '0',
  `disablelogin` text NOT NULL,
  `disableregister` text NOT NULL,
  `keys` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
