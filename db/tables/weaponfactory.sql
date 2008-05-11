CREATE TABLE `weaponfactory` (
  `col` int(11) NOT NULL default '0',
  `count` tinyint(3) unsigned NOT NULL default '0',
  `produces` text NOT NULL,
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Waffenfabriken'; 

ALTER TABLE weaponfactory ADD CONSTRAINT weaponfactory_fk_bases FOREIGN KEY (col) REFERENCES bases(id);