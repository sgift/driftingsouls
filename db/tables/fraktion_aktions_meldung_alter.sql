alter table fraktion_aktions_meldung
add constraint fraktion_aktions_meldung_fk_users
foreign key (gemeldetVon_id)
references users (id);

alter table fraktion_aktions_meldung
add constraint fraktion_aktions_meldung_fk_users2
foreign key (fraktion_id)
references users (id);