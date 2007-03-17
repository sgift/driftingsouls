CREATE TABLE `offiziere` (
  `id` int(11) NOT NULL auto_increment,
  `userid` int(11) NOT NULL default '0',
  `name` varchar(60) NOT NULL default 'noname',
  `rang` tinyint(4) NOT NULL default '0',
  `ing` int(11) NOT NULL default '0',
  `waf` int(11) NOT NULL default '0',
  `nav` int(11) NOT NULL default '0',
  `sec` int(11) NOT NULL default '0',
  `com` int(11) NOT NULL default '0',
  `dest` varchar(15) NOT NULL default '',
  `ingu` int(11) NOT NULL default '0',
  `wafu` int(11) NOT NULL default '0',
  `navu` int(11) NOT NULL default '0',
  `secu` int(11) NOT NULL default '0',
  `comu` int(11) NOT NULL default '0',
  `spec` tinyint(4) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `dest` (`dest`),
  KEY `userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE offiziere ADD CONSTRAINT offiziere_fk_users FOREIGN KEY (userid) REFERENCES users(id);