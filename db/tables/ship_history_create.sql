CREATE TABLE ship_history (
    		id INT NOT NULL,
    		history TEXT,
    		PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
ALTER TABLE ship_history ADD CONSTRAINT ship_history_fk_ships FOREIGN KEY (id) REFERENCES ships(id);