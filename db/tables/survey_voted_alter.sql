ALTER TABLE survey_voted ADD CONSTRAINT survey_voted_fk_surveys FOREIGN KEY (survey_id) REFERENCES surveys(id);
ALTER TABLE survey_voted ADD CONSTRAINT survey_voted_fk_users FOREIGN KEY (user_id) REFERENCES users(id);
