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


INSERT INTO `orders_offiziere` (`id`, `name`, `rang`, `ing`, `waf`, `nav`, `sec`, `com`, `cost`) VALUES (1, 'Ingenieur', 0, 25, 20, 10, 5, 5, 1);
INSERT INTO `orders_offiziere` (`id`, `name`, `rang`, `ing`, `waf`, `nav`, `sec`, `com`, `cost`) VALUES (2, 'Navigator', 0, 5, 10, 30, 5, 10, 1);
INSERT INTO `orders_offiziere` (`id`, `name`, `rang`, `ing`, `waf`, `nav`, `sec`, `com`, `cost`) VALUES (3, 'Sicherheitsexperte', 0, 10, 25, 5, 35, 5, 1);
INSERT INTO `orders_offiziere` (`id`, `name`, `rang`, `ing`, `waf`, `nav`, `sec`, `com`, `cost`) VALUES (4, 'Captain', 0, 10, 10, 15, 5, 35, 1);
     