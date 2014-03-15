alter table skn add index skn_fk_users (userid), add constraint skn_fk_users foreign key (userid) references users (id);
alter table skn add index skn_fk_skn_channels (channel), add constraint skn_fk_skn_channels foreign key (channel) references skn_channels (id);
