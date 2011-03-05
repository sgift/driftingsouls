CREATE TABLE `factory` (
  			`id` int(11) NOT NULL auto_increment,
  			`col` int(11) NOT NULL default '0',
  			`count` tinyint(3) unsigned NOT NULL default '0',
  			`produces` text NOT NULL,
  			`buildingid` int(11) NOT NULL,
  			`version` int(10) unsigned NOT NULL default '0',
  			PRIMARY KEY  (`id`)
		) COMMENT = "Die Fabriken";
		
ALTER TABLE factory ADD UNIQUE col_buildingid_idx (col, buildingid);
ALTER TABLE factory ADD CONSTRAINT factory_fk_bases FOREIGN KEY (col) REFERENCES bases(id);