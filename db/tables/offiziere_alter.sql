ALTER TABLE offiziere ADD CONSTRAINT offiziere_fk_users FOREIGN KEY (userid) REFERENCES users(id);
