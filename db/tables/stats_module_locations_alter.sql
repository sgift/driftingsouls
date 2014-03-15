alter table stats_module_locations add index stats_module_locations_fk_user_id (user_id), add constraint stats_module_locations_fk_user_id foreign key (user_id) references users (id);
