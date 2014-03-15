create index user on skn_visits (user, channel);
alter table skn_visits add index skn_visits_fk_users (user), add constraint skn_visits_fk_users foreign key (user) references users (id);
alter table skn_visits add index skn_visits_fk_skn_channels (channel), add constraint skn_visits_fk_skn_channels foreign key (channel) references skn_channels (id);
