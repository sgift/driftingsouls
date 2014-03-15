alter table battles add index battles_fk_users1 (commander1), add constraint battles_fk_users1 foreign key (commander1) references users (id);
alter table battles add index battles_fk_users2 (commander2), add constraint battles_fk_users2 foreign key (commander2) references users (id);
create index coords on battles (x, y, system);