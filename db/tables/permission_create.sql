CREATE TABLE permission (
	id INT NOT NULL auto_increment,
	user_id INT NOT NULL,
	category VARCHAR(64) NOT NULL,
	action VARCHAR(255) NOT NULL,
	PRIMARY KEY(id)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_bin;