alter table fz add index fz_fk_forschungen (forschung), add constraint fz_fk_forschungen foreign key (forschung) references forschungen (id);
