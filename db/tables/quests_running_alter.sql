create index questid on quests_running (questid, userid);
ALTER TABLE quests_running ADD CONSTRAINT quests_running_fk_users FOREIGN KEY (userid) REFERENCES users(id);
ALTER TABLE quests_running ADD CONSTRAINT quests_running_fk_quests FOREIGN KEY (questid) REFERENCES quests(id);
