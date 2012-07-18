CREATE TABLE schiff_einstellungen (
	`id` int(11) NOT NULL AUTO_INCREMENT,
	`version` int(10) unsigned NOT NULL DEFAULT '0',
	`destsystem` int(11) NOT NULL DEFAULT '0',
	`destx` int(11) NOT NULL DEFAULT '0',
	`desty` int(11) NOT NULL DEFAULT '0',
	`destcom` text NOT NULL,
	`bookmark` tinyint(1) unsigned NOT NULL DEFAULT '0',
	`autodeut` tinyint(3) unsigned NOT NULL DEFAULT '1',
    `startFighters` tinyint(3) unsigned NOT NULL DEFAULT '0',
    `isfeeding` tinyint(1) NOT NULL DEFAULT '0',
	`isallyfeeding` tinyint(1) NOT NULL DEFAULT '0',
	`showtradepost` tinyint(4) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`),
	KEY `bookmark` (`bookmark`),
	KEY `idx_feeding` (`isfeeding`),
	KEY `idx_allyfeeding` (`isallyfeeding`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 PACK_KEYS=0;