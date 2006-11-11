CREATE TABLE `users` (
  `id` int(11) NOT NULL default '0',
  `un` varchar(40) NOT NULL default '',
  `name` varchar(255) NOT NULL default 'Kolonist',
  `passwort` varchar(40) NOT NULL default '',
  `race` int(11) NOT NULL default '1',
  `inakt` int(11) NOT NULL default '0',
  `signup` int(11) NOT NULL default '0',
  `history` text NOT NULL,
  `medals` varchar(50) NOT NULL default '',
  `rang` tinyint(3) unsigned NOT NULL default '0',
  `ally` int(11) NOT NULL default '0',
  `konto` bigint(20) unsigned NOT NULL default '0',
  `cargo` text NOT NULL,
  `nstat` varchar(15) NOT NULL default '0',
  `email` varchar(60) NOT NULL default '',
  `log_fail` mediumint(9) NOT NULL default '0',
  `accesslevel` int(11) NOT NULL default '0',
  `npcpunkte` int(11) NOT NULL default '10',
  `nickname` varchar(255) NOT NULL default 'Kolonist',
  `plainname` varchar(255) NOT NULL default 'Kolonist',
  `allyposten` int(11) NOT NULL default '0',
  `gtudropzone` tinyint(3) unsigned NOT NULL default '2',
  `npcorderloc` varchar(12) NOT NULL default '',
  `imgpath` varchar(200) NOT NULL default 'http://localhost/ds2/',
  `flagschiff` mediumint(8) unsigned NOT NULL default '0',
  `disabled` tinyint(3) unsigned NOT NULL default '0',
  `flags` tinytext NOT NULL,
  `vaccount` smallint(5) unsigned NOT NULL default '0',
  `wait4vac` smallint(5) unsigned NOT NULL default '0',
  `lostBattles` smallint(5) unsigned NOT NULL default '0',
  `wonBattles` smallint(5) unsigned NOT NULL default '0',
  `destroyedShips` int(10) unsigned NOT NULL default '0',
  `lostShips` int(10) unsigned NOT NULL default '0',
  `knownItems` text NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `ally` (`ally`),
  KEY `vaccount` (`vaccount`,`wait4vac`),
  KEY `un` (`un`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-26, 'niemandundnochnichtniemand', 'Ito Kitami - sales agent', '98f6bcd4621d373cade4e832627b4f6', 2, 44, 0, 'Dabei seit dem 25. Tick in DS1', '', 0, 0, 308442632, '100000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 'ito@localhost', 4, 0, 10, 'Ito Kitami - sales agent', 'Ito Kitami - sales agent', 0, 2, '', 'http://ds.drifting-souls.net/', 0, 0, ' NO_IP_SESS NO_AUTOLOGOUT execnotes superdock miljumps', 0, 0, 0, 2, 2, 0, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-19, 'malniemandundmalnicht', 'Demolition Incorporated', '98f6bcd4621d373cade4e832627b4f6', 2, 67, 0, 'Dabei von Anfang an', '', 0, 0, 265353532, '1000000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 'di@localhost', 10, 50, 0, 'Demolition Incorporated', 'Demolition Incorporated', 0, 2, '', 'http://ds.drifting-souls.net/', 124297, 0, 'miljumps superdock \r\n NO_AUTOLOGOUT NO_IP_SESS', 0, 0, 1, 2, 111, 7, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-15, 'hoffentlichbaldniemand', '[font=Roman][color=silver][b]GCoP[/b][/color][/font] Alonso de Mercado', '98f6bcd4621d373cade4e832627b4f6', 2, 68, 0, 'Dabei von Anfang an', '', 0, 0, 46854188, '100000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 'deine_schiffe_sind_meine_schiffe@localhost', 22, 0, 1120, 'Alonso de Mercado', 'GCoP Alonso de Mercado', 0, 2, '', 'http://ds.drifting-souls.net/', 212357, 0, 'miljumps ordermenu  viewsystems execnotes\r\nNO_IP_SESS NO_AUTOLOGOUT', 0, 0, 214, 84, 2437, 2748, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-10, 'manchmalniemand', '[font=BankGothic Md BT]GCP Senat[/font]', '98f6bcd4621d373cade4e832627b4f6', 0, 66, 0, 'Dabei von Anfang an', '', 0, 1, 102261835, '100000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 'senat@localhost', 7, 50, 5745, 'Senat', 'GCP Senat', 0, 2, '', 'http://ds.drifting-souls.net/', 80602, 0, 'miljumps viewbattles ordermenu superdock viewallsystems execnotes NO_IP_SESS NO_AUTOLOGOUT nojnblock ', 0, 0, 0, 0, 460, 475, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-6, 'garnichtniemand', '[color=red][b]Sh Van[/b][/color]', '98f6bcd4621d373cade4e832627b4f6', 3, 67, 0, 'Dabei von Anfang an', '', 0, 0, 3603357, '10000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 'dein_ende@localhost', 2, 15, 5582, '[color=red][b]Sh Van[/b][/color]', 'Sh Van', 0, 2, '', 'http://ds.drifting-souls.net/', 0, 0, 'hide noshipconsign ordermenu NO_IP_SESS NO_AUTOLOGGOUT NO_AUTOLOGOUT', 0, 0, 35, 177, 3979, 3231, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-4, 'nichtniemand', 'Galactic News Network', '98f6bcd4621d373cade4e832627b4f6', 1, 76, 0, 'Dabei von Anfang an', '', 0, 0, 86560384, '10000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,,', '1000', 'gnn@localhost', 59, 0, 5441, 'Galactic News Network', 'Galactic News Network', 0, 2, '', 'http://ds.drifting-souls.net/', 0, 0, 'ordermenu execnotes superdock\r\n NO_AUTOLOGOUT NO_IP_SESS', 0, 0, 19, 3, 18, 25, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-2, 'fastniemand', 'Galtracorp Unlimited', '98f6bcd4621d373cade4e832627b4f6', 1, 13, 0, 'Dabei von Anfang an', '', 0, 0, 2147482268, '100000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 're@localhost', 10, 50, 5895, 'Galtracorp Unlimited', 'Galtracorp Unlimited', 0, 2, '', 'http://localhost/ds2/', 11437, 0, 'miljumps ordermenu superdock viewallsystems execnotes nojnblock noactionblocking NO_IP_SESS NO_AUTOLOGOUT scriptdebug', 0, 0, 8, 42, 596, 139, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (-1, 'niemand', 'Niemand', '98f6bcd4621d373cade4e832627b4f6', 1, 0, 0, 'Dabei von Anfang an', '', 0, 0, 5006954, '100000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,,', '1000', 'niemand@localhost', 1, 15, 5910, 'Niemand', 'Niemand', 0, 2, '', 'http://ds.drifting-souls.net/', 0, 0, 'miljumps viewbattles ordermenu superdock viewallsystems execnotes hide noactionblocking NO_IP_SESS NO_AUTOLOGOUT', 0, 0, 233, 12, 25, 392, '');
INSERT INTO `users` (`id`, `un`, `name`, `passwort`, `race`, `inakt`, `signup`, `history`, `medals`, `rang`, `ally`, `konto`, `cargo`, `nstat`, `email`, `log_fail`, `accesslevel`, `npcpunkte`, `nickname`, `plainname`, `allyposten`, `gtudropzone`, `npcorderloc`, `imgpath`, `flagschiff`, `disabled`, `flags`, `vaccount`, `wait4vac`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`, `knownItems`) VALUES (0, 'nichts', '-', 'abcdef01234567890', 1, 90, 0, 'Dabei von Anfang an', '', 0, 0, 232754, '1000000000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,', '1000', 'Test', 306, 15, 5910, '-', '-', 0, 2, '', 'http://ds.drifting-souls.net/', 0, 0, 'hide ordermenu', 0, 0, 15, 4, 0, 1635, '');