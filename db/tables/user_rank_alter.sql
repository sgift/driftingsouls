alter table user_rank add index user_rank_fk_users1 (owner), add constraint user_rank_fk_users1 foreign key (owner) references users (id);
alter table user_rank add index user_rank_fk_users2 (rank_giver), add constraint user_rank_fk_users2 foreign key (rank_giver) references users (id);
