ALTER TABLE user_values ADD CONSTRAINT user_values_fk_users FOREIGN KEY (`user_id`) REFERENCES users(id);
