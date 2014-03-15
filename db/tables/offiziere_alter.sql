alter table offiziere add index offiziere_fk_bases (stationiertAufBasis_id), add constraint offiziere_fk_bases foreign key (stationiertAufBasis_id) references bases (id);
alter table offiziere add index offiziere_fk_ships (stationiertAufSchiff_id), add constraint offiziere_fk_ships foreign key (stationiertAufSchiff_id) references ships (id);
alter table offiziere add index offiziere_fk_users (userid), add constraint offiziere_fk_users foreign key (userid) references users (id);
