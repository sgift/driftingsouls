CREATE TABLE ally_rangdescriptors (
	id INT NOT NULL auto_increment,
	version INT NOT NULL DEFAULT 0,
	ally_id INT NOT NULL,
	rang INT NOT NULL DEFAULT 0,
	name VARCHAR(255) NOT NULL DEFAULT "",
	customImg VARCHAR(255) DEFAULT NULL,
	PRIMARY KEY(id)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_bin;