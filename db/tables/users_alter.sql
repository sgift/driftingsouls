create index vaccount on users (vaccount, wait4vac);
create index un on users (un);
alter table users add index users_fk_ally_posten (allyposten), add constraint users_fk_ally_posten foreign key (allyposten) references ally_posten (id);
alter table users add index users_fk_ally (ally), add constraint users_fk_ally foreign key (ally) references ally (id);
