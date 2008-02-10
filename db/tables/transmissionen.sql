CREATE TABLE `transmissionen` (
  `id` int(11) NOT NULL auto_increment,
  `gelesen` int(11) NOT NULL default '0',
  `sender` int(11) NOT NULL default '0',
  `empfaenger` int(11) NOT NULL default '0',
  `title` varchar(100) NOT NULL default '',
  `time` int(11) NOT NULL default '0',
  `ordner` int(10) unsigned NOT NULL default '0',
  `flags` int(11) NOT NULL default '0',
  `inhalt` text NOT NULL,
  `kommentar` text NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY `empfaenger` (`empfaenger`,`gelesen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE transmissionen ADD CONSTRAINT transmissionen_fk_users1 FOREIGN KEY (sender) REFERENCES users(id);
ALTER TABLE transmissionen ADD CONSTRAINT transmissionen_fk_users2 FOREIGN KEY (empfaenger) REFERENCES users(id);