CREATE TABLE `portal_links` (
  `id` smallint(5) unsigned NOT NULL auto_increment,
  `url` varchar(250) NOT NULL default '',
  `name` varchar(120) NOT NULL default '',
  `descrip` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Links';
 
