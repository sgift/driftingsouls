create index questid on quests_running (questid, userid);
alter table quests_running add index quests_running_fk_quests (questid), add constraint quests_running_fk_quests foreign key (questid) references quests (id);
alter table quests_running add index quests_running_fk_users (userid), add constraint quests_running_fk_users foreign key (userid) references users (id);
