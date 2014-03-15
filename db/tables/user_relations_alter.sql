create index user_id on user_relations (user_id, target_id);
alter table user_relations add index user_relations_fk_users1 (user_id), add constraint user_relations_fk_users1 foreign key (user_id) references users (id);
alter table user_relations add index user_relations_fk_users2 (target_id), add constraint user_relations_fk_users2 foreign key (target_id) references users (id);