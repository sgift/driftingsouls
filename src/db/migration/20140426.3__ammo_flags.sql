create table ammo_flags (Ammo_id integer not null, flags integer) ENGINE=InnoDB;
alter table ammo_flags add index ammo_flag_fk_ammo (Ammo_id), add constraint ammo_flag_fk_ammo foreign key (Ammo_id) references ammo (id);

insert into ammo_flags select id as Ammo_id, 0 as flags from ammo where (flags&1)=1;
insert into ammo_flags select id as Ammo_id, 1 as flags from ammo where (flags&2)=2;

alter table ammo drop column flags;