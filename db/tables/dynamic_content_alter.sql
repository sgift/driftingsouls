alter table dynamic_content add index dynamic_content_fk_users (hochgeladenDurch_id), add constraint dynamic_content_fk_users foreign key (hochgeladenDurch_id) references users (id);
