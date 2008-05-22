ALTER TABLE survey_entries ADD CONSTRAINT survey_entries_fk_surveys FOREIGN KEY (survey_id) REFERENCES surveys(id);
