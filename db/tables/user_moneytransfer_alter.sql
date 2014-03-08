create index from on user_moneytransfer (from_id, to_id);
ALTER TABLE user_moneytransfer ADD CONSTRAINT user_moneytransfer_fk_users1 FOREIGN KEY (`from_id`) REFERENCES users(id);
ALTER TABLE user_moneytransfer ADD CONSTRAINT user_moneytransfer_fk_users2 FOREIGN KEY (`to_id`) REFERENCES users(id);
