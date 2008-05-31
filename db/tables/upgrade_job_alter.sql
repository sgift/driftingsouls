ALTER TABLE upgrade_job ADD CONSTRAINT upgrade_job_fk_base FOREIGN KEY (baseid) REFERENCES bases(id);
ALTER TABLE upgrade_job ADD CONSTRAINT upgrade_job_fk_user FOREIGN KEY (userid) REFERENCES users(id);
ALTER TABLE upgrade_job ADD CONSTRAINT upgrade_job_fk_ships FOREIGN KEY (colonizerid) REFERENCES ships(id);
ALTER TABLE upgrade_job ADD CONSTRAINT upgrade_job_fk_mod_tiles FOREIGN KEY (tiles) REFERENCES upgrade_info(id);
ALTER TABLE upgrade_job ADD CONSTRAINT upgrade_job_fk_mod_cargo FOREIGN KEY (cargo) REFERENCES upgrade_info(id);
