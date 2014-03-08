create index id on user_values (user_id, name);
ALTER TABLE user_values ADD CONSTRAINT user_values_fk_users FOREIGN KEY (`user_id`) REFERENCES users(id);
