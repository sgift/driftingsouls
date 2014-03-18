alter table werften add index werften_fk_werften (linkedWerft), add constraint werften_fk_werften foreign key (linkedWerft) references werften (id);
alter table werften add index werften_fk_ships (shipid), add constraint werften_fk_ships foreign key (shipid) references ships (id);
alter table werften add index werften_fk_bases (linked), add constraint werften_fk_bases foreign key (linked) references bases (id)
