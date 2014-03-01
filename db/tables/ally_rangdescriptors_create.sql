CREATE TABLE ally_rangdescriptors (
	id integer not null auto_increment,
	customImg varchar(255),
	name varchar(255) not null,
	rang integer not null,
	version integer not null,
	ally_id integer not null,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;