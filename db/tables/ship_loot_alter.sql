create index shiptype on ship_loot (shiptype);
alter table ship_loot add index ship_loot_fk_users2 (targetuser), add constraint ship_loot_fk_users2 foreign key (targetuser) references users (id);
alter table ship_loot add index ship_loot_fk_users1 (owner), add constraint ship_loot_fk_users1 foreign key (owner) references users (id);