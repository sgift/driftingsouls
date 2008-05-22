CREATE TABLE `smilies` (
  `id` smallint(6) NOT NULL auto_increment,
  `tag` varchar(10) NOT NULL default '',
  `image` varchar(40) NOT NULL default '',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Smilies'; 
