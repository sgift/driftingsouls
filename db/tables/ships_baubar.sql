CREATE TABLE `ships_baubar` (
  `id` int(11) NOT NULL auto_increment,
  `type` int(11) NOT NULL default '0',
  `costs` varchar(100) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0',
  `linfactor` float NOT NULL default '0',
  `crew` smallint(6) NOT NULL default '0',
  `dauer` tinyint(4) NOT NULL default '0',
  `ekosten` int(4) NOT NULL default '0',
  `race` int(11) NOT NULL default '0',
  `systemreq` int(11) NOT NULL default '0',
  `tr1` int(11) NOT NULL default '0',
  `tr2` int(11) NOT NULL default '0',
  `tr3` int(11) NOT NULL default '0',
  `werftreq` tinytext NOT NULL,
  `flagschiff` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (5, 12, '0,0,276,189,0,0,0,36,12,0,0,4,0,0,0,0,0,0,0', 0, 20, 9, 78, 2, 1, 7, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (6, 2, '0,0,175,135,0,0,25,52,17,0,0,0,0,0,0,0,0,0,0', 0, 10, 6, 87, 1, 0, 1, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (13, 13, '0,0,37,56,0,0,3,26,19,0,0,0,0,0,0,0,0,0,0', 0, 0, 2, 46, 1, 1, 12, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (14, 28, '0,0,27,23,0,0,4,28,4,0,0,0,0,0,0,0,0,0,0', 0, 0, 1, 22, 1, 0, 32, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (15, 29, '0,0,25,21,0,0,3,38,4,0,0,0,0,0,0,0,0,0,0', 0, 0, 1, 26, 2, 0, 32, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (16, 30, '0,0,185,125,0,0,30,72,21,0,0,0,0,0,0,0,0,0,0', 0, 10, 6, 93, 2, 0, 1, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (23, 5, '0,0,738,566,0,0,702,264,378,298,0,68,0,0,0,0,0,0,0', 0, 140, 34, 389, 1, 1, 23, 31, 29, 'ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (25, 8, '0,0,694,578,0,0,304,134,70,134,0,16,0,0,0,0,0,0,0', 0, 30, 16, 371, 1, 1, 34, 0, 0, 'ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (28, 44, '0,0,192,56,0,0,112,278,198,58,0,4,0,0,0,0,0,0,0', 0, 20, 9, 387, 1, 0, 30, 10, 23, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (30, 33, '0,0,1000,2000,0,0,1000,1000,500,800,0,30,0,0,0,0,0,0,0', 0.5, 120, 120, 700, 0, 0, 33, 0, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (31, 14, '0,0,376,448,0,0,388,272,140,392,0,12,0,0,0,0,0,0,0', 0, 5, 10, 212, 0, 1, 19, 0, 0, 'ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (33, 51, '0,0,600,1200,0,0,856,1240,456,764,0,85,0,0,0,0,0,0,0', 0, 140, 60, 800, 0, 1, 43, 0, 0, 'ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (34, 6, '0,0,930,68,0,0,462,762,822,596,0,68,0,0,0,0,0,0,0', 0, 75, 25, 512, 2, 1, 34, 0, 0, 'ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (39, 54, '0,0,100,80,0,0,168,64,48,0,0,8,0,0,0,0,0,0,0', 0, 8, 3, 200, 1, 1, 21, 4, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (42, 9, '0,0,320,200,0,0,50,150,20,250,0,2,0,0,0,0,0,0,0', 0, 10, 12, 150, 0, 0, 36, 63, 0, 'pwerft ganymed', 0);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (48, 1, '0,0,2567,2540,0,0,3450,1100,1240,2890,0,395,0,0,0,0,0,0,0', 0, 280, 110, 1150, 1, 1, 65, 0, 0, 'ganymed', 1);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (49, 7, '0,0,2890,2358,0,0,3600,1400,1200,2980,0,400,0,0,0,0,0,0,0', 0, 250, 110, 1180, 2, 1, 65, 0, 0, 'ganymed', 1);
INSERT INTO `ships_baubar` (`id`, `type`, `costs`, `linfactor`, `crew`, `dauer`, `ekosten`, `race`, `systemreq`, `tr1`, `tr2`, `tr3`, `werftreq`, `flagschiff`) VALUES (57, 56, '0,0,144,80,0,0,12,84,92,0,0,4,0,0,0,0,0,0,0', 0, 4, 2, 208, 1, 1, 15, 0, 0, 'pwerft ganymed', 0);