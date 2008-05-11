CREATE TABLE `ally` (
  `id` int(11) NOT NULL auto_increment,
  `name` text NOT NULL,
  `plainname` varchar(255) NOT NULL default '',
  `founded` datetime NOT NULL default '0000-00-00 00:00:00',
  `tick` int(11) NOT NULL default '0',
  `president` int(11) NOT NULL default '0',
  `description` text NOT NULL,
  `hp` text NOT NULL,
  `allytag` varchar(120) NOT NULL default '[name]',
  `showastis` tinyint(1) unsigned NOT NULL default '1',
  `showGtuBieter` tinyint(3) unsigned NOT NULL default '0',
  `showlrs` tinyint(3) unsigned NOT NULL default '1',
  `pname` varchar(100) NOT NULL default 'Pr&auml;sident',
  `items` text NOT NULL,
  `lostBattles` smallint(5) unsigned NOT NULL default '0',
  `wonBattles` smallint(5) unsigned NOT NULL default '0',
  `destroyedShips` int(10) unsigned NOT NULL default '0',
  `lostShips` int(10) unsigned NOT NULL default '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE ally ADD CONSTRAINT ally_fk_users FOREIGN KEY (president) REFERENCES users(id);

INSERT INTO `ally` (`id`, `name`, `plainname`, `founded`, `tick`, `president`, `description`, `hp`, `allytag`, `showastis`, `showGtuBieter`, `showlrs`, `pname`, `items`, `lostBattles`, `wonBattles`, `destroyedShips`, `lostShips`) VALUES (1, 'Galactic Commonwealth of Planets', 'Galactic Commonwealth of Planets', '2372-01-01 00:00:00', 28, -10, 'Wir treten die schwere Nachfolge der alten galaktischen Allianz GTVA an.\r\nGeschwÃ¤cht durch die ZestÃ¶rung der Heimat der Vasudaner, den feigen shivanischen Angriff auf Terra und den Verlust des Capella-Systems, gestÃ¤rkt durch die beiden neuen VÃ¶lker des Commonwealth werden wir diesen entvÃ¶lkerten Sektoren Frieden und Schutz bringen.\r\nDie destruktiven Elemente der xenophoben Revanchisten der NTF, die Extremisten der HoL und alle anderen Spezieszisten werden wir nur einmal warnen:\r\nEntehren Sie nicht die Gefallenen unserer beiden VÃ¶lker - sowohl meine BrÃ¼der und Schwestern haben bei der Verteidigung Terras Zehntausende Tote zu beklagen gehabt, wie auch unsere terranischen WaffenbrÃ¼der ihre Leben fÃ¼r die alte Heimat der Vasudaner gaben. Sie fragten nicht nach SpezieszugehÃ¶rigkeit und dem Warum - Sie handelten in dem Geist des neuen BÃ¼ndnis'' - stellvertretend fÃ¼r jeden von uns.', '', '[font=BankGothic Md BT]GCP [name][/font]', 1, 0, 0, 'Exekutivrat', '', 45, 210, 6663, 4382);