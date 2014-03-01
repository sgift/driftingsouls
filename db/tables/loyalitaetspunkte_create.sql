CREATE TABLE loyalitaetspunkte (
	id integer not null auto_increment,
	anmerkungen longtext,
	anzahlPunkte integer NOT NULL,
	grund varchar(255) not null,
	zeitpunkt datetime not null,
	user_id integer not null,
	verliehenDurch_id integer not null,
	primary key (id)
) ENGINE=InnoDB;