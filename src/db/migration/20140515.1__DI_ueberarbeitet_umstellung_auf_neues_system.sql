alter table upgrade_maxvalues add column maximalwert integer not null;
alter table upgrade_maxvalues add column upgradetype integer not null;
alter table upgrade_maxvalues add column type_id integer not null;

CREATE TABLE `dummy` (
`id`  int(11) NOT NULL AUTO_INCREMENT ,
`upgradetype`  integer not null ,
`maximalwert`  integer NOT NULL ,
`type_id`  integer NOT NULL ,
PRIMARY KEY (`id`)
)
ENGINE=InnoDB
;

insert into dummy(upgradetype,maximalwert, type_id) select 1, maxtiles, type from upgrade_maxvalues;
insert into dummy(upgradetype,maximalwert, type_id) select 2, maxcargo, type from upgrade_maxvalues;

delete from upgrade_maxvalues;

alter table upgrade_maxvalues add UNIQUE KEY `type_id` (`type_id`,`upgradetype`);
alter table upgrade_maxvalues drop foreign key upgrade_max_values_fk_basetype;
alter table upgrade_maxvalues drop maxtiles;
alter table upgrade_maxvalues drop maxcargo;
alter table upgrade_maxvalues drop type;

insert into upgrade_maxvalues(upgradetype, maximalwert, type_id) select upgradetype, maximalwert, type_id from dummy;
drop table dummy;

alter table upgrade_info add column maxticks integer not null;
alter table upgrade_info add column minticks integer not null;
alter table upgrade_info add column upgradetype integer not null;

update upgrade_info set upgradetype=1 where cargo=0;
update upgrade_info set upgradetype=2 where cargo=1;
update upgrade_info set maxticks=70, minticks=7;

alter table upgrade_info drop cargo;

create table upgrade_job_upgrade_info (upgrade_job_id integer not null, upgradelist_id integer not null, PRIMARY KEY (`upgrade_job_id`,`upgradelist_id`)) ENGINE=InnoDB;
alter table upgrade_job_upgrade_info add index upgrade_info_fk_upgrade_job (upgradelist_id), add constraint upgrade_info_fk_upgrade_job foreign key (upgradelist_id) references upgrade_info (id);
alter table upgrade_job_upgrade_info add index upgrade_job_fk_mod (upgrade_job_id), add constraint upgrade_job_fk_mod foreign key (upgrade_job_id) references upgrade_job (id);

insert into upgrade_job_upgrade_info select id, tiles from upgrade_job;
insert into upgrade_job_upgrade_info select id, cargo from upgrade_job;

alter table upgrade_job drop foreign key upgrade_job_fk_mod_tiles;
alter table upgrade_job drop foreign key upgrade_job_fk_mod_cargo;
alter table upgrade_job drop tiles;
alter table upgrade_job drop cargo;

delete from upgrade_job_upgrade_info where upgradelist_id in (select id from upgrade_info where modwert=0);
delete from upgrade_info where modwert = 0;