ALTER TABLE user_moneytransfer ADD CONSTRAINT user_moneytransfer_fk_users1 FOREIGN KEY (`from`) REFERENCES users(id);
ALTER TABLE user_moneytransfer ADD CONSTRAINT user_moneytransfer_fk_users2 FOREIGN KEY (`to`) REFERENCES users(id);
