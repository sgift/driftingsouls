CREATE TABLE `handel` (
  `id` int(11) NOT NULL auto_increment,
  `who` int(11) NOT NULL default '0',
  `time` int(11) unsigned NOT NULL default '0',
  `sucht` text NOT NULL,
  `bietet` text NOT NULL,
  `comm` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE handel ADD CONSTRAINT handel_fk_users FOREIGN KEY (who) REFERENCES users(id);