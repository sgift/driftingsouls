create index user_id on user_relations (user_id, target_id);
ALTER TABLE user_relations ADD CONSTRAINT user_relations_fk_users1 FOREIGN KEY (`user_id`) REFERENCES users(id);
ALTER TABLE user_relations ADD CONSTRAINT user_relations_fk_users2 FOREIGN KEY (`target_id`) REFERENCES users(id);
