CREATE TABLE `gtu_warenkurse` (
  `place` varchar(10) NOT NULL default 'asti',
  `name` varchar(30) NOT NULL default '',
  `kurse` text NOT NULL,
  PRIMARY KEY  (`place`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

INSERT INTO `gtu_warenkurse` (`place`, `name`, `kurse`) VALUES ('asti', 'Kommandozentrale', '5,5,50,50,50,2000,1000,500,1000,2000,20,50000,250,50,50000,0,0,0,6|1000|0|0');
INSERT INTO `gtu_warenkurse` (`place`, `name`, `kurse`) VALUES ('tradepost', 'Handelsposten', '100,100,1500,1000,2000,43000,10000,5000,11000,25000,20,200000,500,50,200000,10000000,100000000,0,6|1000|0|0;150|2000|0|0;154|3000|0|0;151|5000|0|0');
