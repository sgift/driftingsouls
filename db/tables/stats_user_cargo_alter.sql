alter table stats_user_cargo add index stats_user_cargo_fk_user_id (user_id), add constraint stats_user_cargo_fk_user_id foreign key (user_id) references users (id);
