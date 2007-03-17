CREATE TABLE `orders_ships` (
  `type` int(11) NOT NULL default '0',
  `cost` int(11) NOT NULL default '0',
  PRIMARY KEY  (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE orders_ships ADD CONSTRAINT orders_ships_fk_ship_types FOREIGN KEY (type) REFERENCES ship_types(id);

INSERT INTO `orders_ships` (`type`, `cost`) VALUES (1, 8);
INSERT INTO `orders_ships` (`type`, `cost`) VALUES (2, 1);
INSERT INTO `orders_ships` (`type`, `cost`) VALUES (3, 12);