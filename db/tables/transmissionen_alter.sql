create index empfaenger on transmissionen (empfaenger, gelesen);
alter table transmissionen add index transmissionen_fk_users1 (sender), add constraint transmissionen_fk_users1 foreign key (sender) references users (id);
alter table transmissionen add index transmissionen_fk_users2 (empfaenger), add constraint transmissionen_fk_users2 foreign key (empfaenger) references users (id);
