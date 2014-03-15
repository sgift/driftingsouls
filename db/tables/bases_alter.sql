create index coords on bases (x, y, system);
create index owner on bases (owner, id);
alter table bases add index bases_fk_academy (academy_id), add constraint bases_fk_academy foreign key (academy_id) references academy (id);
alter table bases add index bases_fk_werften (werft_id), add constraint bases_fk_werften foreign key (werft_id) references werften (id);
alter table bases add index bases_fk_basetypes (klasse), add constraint bases_fk_basetypes foreign key (klasse) references base_types (id);
alter table bases add index bases_fk_fz (forschungszentrum_id), add constraint bases_fk_fz foreign key (forschungszentrum_id) references fz (id);
alter table bases add index bases_fk_users (owner), add constraint bases_fk_users foreign key (owner) references users (id);
