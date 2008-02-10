CREATE TABLE `fz` (
  `col` int(11) NOT NULL default '0',
  `type` int(11) NOT NULL default '0',
  `forschung` int(11) NOT NULL default '0',
  `dauer` int(11) NOT NULL default '0',
  PRIMARY KEY  (`col`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE fz ADD CONSTRAINT fz_fk_bases FOREIGN KEY (col) REFERENCES bases(id);
