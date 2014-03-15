alter table versteigerungen add index versteigerungen_fk_users (bieter), add constraint versteigerungen_fk_users foreign key (bieter) references users(id);
alter table versteigerungen add index versteigerungen_fk_users2 (owner), add constraint versteigerungen_fk_users2 foreign key (owner) references users (id);

