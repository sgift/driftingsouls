create index questid on quests_completed (questid, userid);
alter table quests_completed add index quests_completed_fk_quests (questid), add constraint quests_completed_fk_quests foreign key (questid) references quests (id);
alter table quests_completed add index quests_completed_fk_users (userid), add constraint quests_completed_fk_users foreign key (userid) references users (id);