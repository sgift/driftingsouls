CREATE TABLE `userresearch` (
	`id` int(11) unsigned NOT NULL auto_increment,
	`owner` int(11) NOT NULL,
	`research` int(11) NOT NULL,
	PRIMARY KEY  (`id`),
	UNIQUE (`owner`,`research`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Forschungen der Spieler'; 
ALTER TABLE userresearch ADD CONSTRAINT userresearch_fk_forschungen FOREIGN KEY (research) REFERENCES forschungen(id);

INSERT INTO userresearch (owner,research) VALUES (-26,0);
INSERT INTO userresearch (owner,research) VALUES (-19,0);
INSERT INTO userresearch (owner,research) VALUES (-15,0);
INSERT INTO userresearch (owner,research) VALUES (-10,0);
INSERT INTO userresearch (owner,research) VALUES (-6,0);
INSERT INTO userresearch (owner,research) VALUES (-4,0);
INSERT INTO userresearch (owner,research) VALUES (-2,0);
INSERT INTO userresearch (owner,research) VALUES (-1,0);