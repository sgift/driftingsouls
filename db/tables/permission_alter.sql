alter table permission add index permission_fk_users (user_id), add constraint permission_fk_users foreign key (user_id) references users (id);
