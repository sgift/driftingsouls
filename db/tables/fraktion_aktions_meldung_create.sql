create table fraktion_aktions_meldung (
	id bigint not null auto_increment,
	bearbeitetAm datetime,
	gemeldetAm datetime,
	meldungstext longtext,
	version integer not null,
	fraktion_id integer,
	gemeldetVon_id integer,
	primary key (id)
) ENGINE=InnoDB;