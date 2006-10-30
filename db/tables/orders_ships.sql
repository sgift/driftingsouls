CREATE TABLE `orders_ships` (
  `type` int(11) NOT NULL default '0',
  `cost` int(11) NOT NULL default '0',
  PRIMARY KEY  (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

INSERT INTO `orders_ships` (`type`, `cost`) VALUES (1, 8);
INSERT INTO `orders_ships` (`type`, `cost`) VALUES (2, 1);
INSERT INTO `orders_ships` (`type`, `cost`) VALUES (3, 12);