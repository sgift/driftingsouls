CREATE TABLE `weaponfactory` (
  `col` int(11) NOT NULL default '0',
  `count` tinyint(3) unsigned NOT NULL default '0',
  `produces` text NOT NULL,
  PRIMARY KEY  (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die Waffenfabriken'; 

ALTER TABLE weaponfactory ADD CONSTRAINT weaponfactory_fk_bases FOREIGN KEY (col) REFERENCES bases(id);