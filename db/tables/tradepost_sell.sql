CREATE TABLE `tradepost_sell` (
	`shipid` int(11) NOT NULL,
	`resourceid` int(11) NOT NULL,
	`price` int(11) NOT NULL,
	`limit` int(11) NOT NULL,
	PRIMARY KEY  (`shipid`,`resourceid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
		
ALTER TABLE tradepost_sell ADD CONSTRAINT tradepost_sell_fk_ships FOREIGN KEY (shipid) REFERENCES ships(id);