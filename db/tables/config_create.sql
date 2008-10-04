CREATE TABLE config (
	`name` VARCHAR( 25 ) NOT NULL ,
	`value` TEXT NOT NULL ,
	`description` TEXT NOT NULL ,
	`version` INT NOT NULL DEFAULT 0,
	PRIMARY KEY (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;