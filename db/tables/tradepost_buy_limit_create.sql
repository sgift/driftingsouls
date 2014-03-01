CREATE TABLE `tradepost_buy_limit` (
  `shipid` integer NOT NULL,
  `resourceid` integer NOT NULL,
  `maximum` integer NOT NULL,
  `min_rank` integer NOT NULL DEFAULT  '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`shipid`,`resourceid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
