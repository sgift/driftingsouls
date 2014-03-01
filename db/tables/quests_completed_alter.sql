ALTER TABLE quests_completed ADD CONSTRAINT quests_completed_fk_users FOREIGN KEY (userid) REFERENCES users(id);
ALTER TABLE quests_completed ADD CONSTRAINT quests_completed_fk_quests FOREIGN KEY (questid) REFERENCES quests(id);
create index questid on quests_completed (questid, userid);