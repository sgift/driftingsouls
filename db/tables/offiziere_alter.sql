ALTER TABLE offiziere
	ADD CONSTRAINT offiziere_fk_users FOREIGN KEY (userid) REFERENCES users(id),
	ADD CONSTRAINT offiziere_fk_ships FOREIGN KEY (stationiertAufSchiff_id) REFERENCES ships(id),
	ADD CONSTRAINT offiziere_fk_bases FOREIGN KEY (stationiertAufBasis_id) REFERENCES bases(id);