create table dynamic_content (
	id varchar(255) not null,
	anlagedatum datetime,
	aenderungsdatum datetime,
	autor varchar(255),
	lizenz varchar(255),
	lizenzdetails TEXT,
	quelle varchar(255),
	version integer not null,
	hochgeladenDurch_id integer,
	primary key (id)
) ENGINE=InnoDB;