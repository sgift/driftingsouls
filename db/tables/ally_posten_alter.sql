alter table ally_posten add index ally_posten_fk_ally (ally), add constraint ally_posten_fk_ally foreign key (ally) references ally(id);
