ALTER TABLE survey_results ADD CONSTRAINT survey_results_fk_surveys FOREIGN KEY (survey_id) REFERENCES surveys(id);
