CREATE TABLE `cores` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(50) NOT NULL default 'Noname',
  `astitype` tinyint(3) unsigned NOT NULL default '1',
  `buildcosts` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `consumes` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `produces` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `arbeiter` int(11) NOT NULL default '0',
  `ever` int(11) NOT NULL default '0',
  `eprodu` int(11) NOT NULL default '0',
  `bewohner` int(11) NOT NULL default '0',
  `techreq` int(11) NOT NULL default '0',
  `eps` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (1, 'Autarkie', 1, '0,0,1000,1500,0,0,500,400,300,200,0,20,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '10000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', 0, 0, 20, 4000, 26, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (2, 'Erzverarbeitungskomplex', 1, '0,0,300,1200,0,0,700,600,400,250,0,10,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '800,0,0,40,0,0,30,35,25,15,0,0,0,0,0,0,0,0,', 800, 0, 20, 800, 27, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (3, 'Multifusionskern', 1, '0,400,500,1400,0,0,750,500,350,100,0,15,0,0,0,0,0,0,', '0,40,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', 600, 0, 650, 600, 28, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (4, 'Autarkie', 3, '0,0,1500,2000,0,0,1000,600,600,300,0,30,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '15000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', 0, 0, 40, 6000, 26, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (5, 'Autarkie', 4, '0,0,800,1200,0,0,300,200,100,50,0,20,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '7500,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', 0, 0, 15, 3000, 26, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (6, 'Erzverarbeitungskomplex', 3, '0,0,500,1600,0,0,1000,1000,800,350,0,30,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1200,0,0,70,0,0,40,50,30,25,0,0,0,0,0,0,0,0,', 1200, 0, 30, 1200, 27, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (7, 'Erzverarbeitungskomplex', 4, '0,0,200,900,0,0,500,500,300,200,0,7,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '600,0,0,30,0,0,25,25,20,10,0,0,0,0,0,0,0,0,', 600, 0, 15, 600, 27, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (8, 'Multifusionskern', 3, '0,600,700,1700,0,0,1000,600,500,200,0,30,0,0,0,0,0,0,', '0,55,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', 700, 0, 850, 700, 28, 0);
INSERT INTO `cores` (`id`, `name`, `astitype`, `buildcosts`, `consumes`, `produces`, `arbeiter`, `ever`, `eprodu`, `bewohner`, `techreq`, `eps`) VALUES (9, 'Multifusionskern', 4, '0,300,400,1300,0,0,650,400,250,80,0,10,0,0,0,0,0,0,', '0,30,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', 400, 0, 500, 400, 28, 0);