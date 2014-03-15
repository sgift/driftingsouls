alter table ordner add index ordner_fk_users (playerid), add constraint ordner_fk_users foreign key (playerid) references users (id);
