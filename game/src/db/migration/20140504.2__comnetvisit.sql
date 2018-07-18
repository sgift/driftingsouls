alter table skn_visits drop foreign key skn_visits_fk_users;
alter table skn_visits drop foreign key skn_visits_fk_skn_channels;
alter table skn_visits drop index user;
alter table skn_visits drop index skn_visits_fk_users;
alter table skn_visits drop index skn_visits_fk_skn_channels;

alter table skn_visits change column channel channel_id integer not null;
alter table skn_visits change column user user_id integer not null;

create index skn_visits_user on skn_visits (user_id, channel_id);
alter table skn_visits add index skn_visits_fk_skn_channels (channel_id), add constraint skn_visits_fk_skn_channels foreign key (channel_id) references skn_channels (id);
alter table skn_visits add index skn_visits_fk_users (user_id), add constraint skn_visits_fk_users foreign key (user_id) references users (id);
