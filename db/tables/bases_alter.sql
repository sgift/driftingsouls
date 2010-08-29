ALTER TABLE bases ADD CONSTRAINT bases_fk_users FOREIGN KEY (owner) REFERENCES users(id);
ALTER TABLE bases ADD CONSTRAINT FOREIGN KEY bases_fk_academy (academy_id) REFERENCES academy(id);
ALTER TABLE bases ADD CONSTRAINT FOREIGN KEY bases_fk_fz (forschungszentrum_id) REFERENCES fz(id);
ALTER TABLE bases ADD CONSTRAINT FOREIGN KEY bases_fk_werften (werft_id) REFERENCES werften(id);