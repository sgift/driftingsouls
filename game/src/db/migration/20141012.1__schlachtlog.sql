alter table battles add column schlachtLog_id bigint;
create table schlacht_log (id bigint not null auto_increment, startTick integer not null, startZeitpunkt datetime, system integer not null, version integer, x integer not null, y integer not null, primary key (id)) ENGINE=InnoDB;
create table schlacht_log_eintrag (DTYPE varchar(31) not null, id bigint not null auto_increment, tick integer not null, version integer, zeitpunkt datetime, seite integer, text longtext, allianzId integer, name varchar(255), userId integer, typ integer, schlachtlog_id bigint, primary key (id)) ENGINE=InnoDB;
alter table battles add index battles_fk_schlachtlog (schlachtLog_id), add constraint battles_fk_schlachtlog foreign key (schlachtLog_id) references schlacht_log (id);
alter table schlacht_log_eintrag add index schlachtlogeintrag_fk_schlachtlog (schlachtlog_id), add constraint schlachtlogeintrag_fk_schlachtlog foreign key (schlachtlog_id) references schlacht_log (id);
alter table upgrade_maxvalues add index upgrade_max_values_fk_basetype (type_id), add constraint upgrade_max_values_fk_basetype foreign key (type_id) references base_types (id);
alter table ships_lost add column schlachtLog_id bigint;
alter table ships_lost add index shiplost_fk_schlachtlog (schlachtLog_id), add constraint shiplost_fk_schlachtlog foreign key (schlachtLog_id) references schlacht_log (id);