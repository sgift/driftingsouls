ALTER TABLE user_f ADD CONSTRAINT user_f_fk_users FOREIGN KEY (id) REFERENCES users(id);
