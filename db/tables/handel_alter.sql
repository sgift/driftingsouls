alter table handel add index handel_fk_users (who), add constraint handel_fk_users foreign key (who) references users (id);
