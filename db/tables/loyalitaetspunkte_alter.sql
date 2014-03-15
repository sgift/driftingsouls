alter table loyalitaetspunkte add index loyalitaetspunkte_fk_users_2 (verliehenDurch_id), add constraint loyalitaetspunkte_fk_users_2 foreign key (verliehenDurch_id) references users (id);
alter table loyalitaetspunkte add index loyalitaetspunkte_fk_users_1 (user_id), add constraint loyalitaetspunkte_fk_users_1 foreign key (user_id) references users (id);
