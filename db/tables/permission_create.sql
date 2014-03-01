CREATE TABLE permission (
	id integer not null auto_increment,
	action varchar(255) not null,
	category varchar(255) not null,
	user_id integer not null,
	primary key (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;