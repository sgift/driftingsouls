create index id on user_values (user_id, name);
alter table user_values add index user_values_fk_users (user_id), add constraint user_values_fk_users foreign key (user_id) references users (id);
