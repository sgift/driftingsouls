CREATE TABLE `orders_offiziere` (
  `id` smallint(5) unsigned NOT NULL auto_increment,
  `name` varchar(30) NOT NULL default '',
  `rang` tinyint(3) unsigned NOT NULL default '0',
  `ing` smallint(5) unsigned NOT NULL default '0',
  `waf` smallint(5) unsigned NOT NULL default '0',
  `nav` smallint(5) unsigned NOT NULL default '0',
  `sec` smallint(5) unsigned NOT NULL default '0',
  `com` smallint(5) unsigned NOT NULL default '0',
  `cost` tinyint(3) NOT NULL default '1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Offiziers-templates für NPC-Orders'; 
