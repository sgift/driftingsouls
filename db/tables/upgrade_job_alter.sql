alter table upgrade_job add index upgrade_job_fk_mod_tiles (tiles), add constraint upgrade_job_fk_mod_tiles foreign key (tiles) references upgrade_info (id);
alter table upgrade_job add index upgrade_job_fk_ships (colonizerid), add constraint upgrade_job_fk_ships foreign key (colonizerid) references ships (id);
alter table upgrade_job add index upgrade_job_fk_base (baseid), add constraint upgrade_job_fk_base foreign key (baseid) references bases (id);
alter table upgrade_job add index upgrade_job_fk_mod_cargo (cargo), add constraint upgrade_job_fk_mod_cargo foreign key (cargo) references upgrade_info (id);
alter table upgrade_job add index upgrade_job_fk_user (userid), add constraint upgrade_job_fk_user foreign key (userid) references users (id);
