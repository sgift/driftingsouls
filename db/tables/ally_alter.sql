alter table ally add index ally_fk_users (president), add constraint ally_fk_users foreign key (president) references users(id);
