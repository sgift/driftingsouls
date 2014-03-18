create index from_idx on user_moneytransfer (from_id, to_id);
alter table user_moneytransfer add index user_moneytransfer_fk_users2 (to_id), add constraint user_moneytransfer_fk_users2 foreign key (to_id) references users (id);
alter table user_moneytransfer add index user_moneytransfer_fk_users1 (from_id), add constraint user_moneytransfer_fk_users1 foreign key (from_id) references users (id);
