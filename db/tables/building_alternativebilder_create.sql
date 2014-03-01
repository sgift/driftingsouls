CREATE TABLE building_alternativebilder (
	Building_id integer not null,
	alternativeBilder varchar(255),
	alternativeBilder_KEY integer,
	primary key (Building_id,alternativeBilder_KEY)
) ENGINE=InnoDB;