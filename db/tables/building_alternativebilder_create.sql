CREATE TABLE building_alternativebilder (
	Building_id INT NOT NULL,
	alternativeBilder_KEY INT NOT NULL,
	alternativeBilder VARCHAR(255) NOT NULL,
	PRIMARY KEY(Building_id,alternativeBilder_KEY)
) ENGINE=InnoDB;