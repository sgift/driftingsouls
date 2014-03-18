create index posten on gtu_zwischenlager (posten, user1, user2);
alter table gtu_zwischenlager add index gtu_zwischenlager_fk_users2 (user2), add constraint gtu_zwischenlager_fk_users2 foreign key (user2) references users (id);
alter table gtu_zwischenlager add index gtu_zwischenlager_fk_users1 (user1), add constraint gtu_zwischenlager_fk_users1 foreign key (user1) references users (id);
alter table gtu_zwischenlager add index gtu_zwischenlager_fk_ships (posten), add constraint gtu_zwischenlager_fk_ships foreign key (posten) references ships (id);