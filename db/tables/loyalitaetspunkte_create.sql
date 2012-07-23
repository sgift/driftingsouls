CREATE TABLE loyalitaetspunkte (
	id INT NOT NULL auto_increment,
	user_id INT NOT NULL,
	grund VARCHAR(255) NOT NULL,
	anmerkungen TEXT,
	anzahlPunkte INT NOT NULL,
	zeitpunkt DATETIME NOT NULL,
	verliehenDurch_id INT NOT NULL,
	PRIMARY KEY(id)
) ENGINE=InnoDB;