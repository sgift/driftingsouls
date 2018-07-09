alter table items add column set_id integer;
alter table items add column mods_id bigint;

create table schiffsmodul_slots (Schiffsmodul_id integer not null, slots varchar(255)) ENGINE=InnoDB;
create table schiffstyp_modifikation (id bigint not null auto_increment, aDocks integer not null, ablativeArmor integer not null, bounty decimal(19,2), cargo bigint not null, cost integer not null, crew integer not null, deutFactor integer not null, eps integer not null, heat integer not null, hull integer not null, hydro integer not null, jDocks integer not null, lostInEmpChance double precision not null, maxunitsize integer not null, minCrew integer not null, nahrungcargo bigint not null, nickname varchar(255), oneWayWerft_id integer, panzerung integer not null, pickingCost integer not null, picture varchar(255), ra integer not null, rd integer not null, reCost integer not null, rm integer not null, ru integer not null, scanCost integer not null, sensorRange integer not null, shields integer not null, size integer not null, srs boolean, torpedoDef integer not null, unitspace integer not null, version integer not null, werft integer not null, primary key (id)) ENGINE=InnoDB;
create table schiffswaffenkonfiguration (id bigint not null auto_increment, anzahl integer not null, hitze integer not null, maxUeberhitzung integer not null, version integer not null, waffe_id varchar(255), schiffstyp_modifikation_id bigint, primary key (id)) ENGINE=InnoDB;

alter table items add index schiffsmodul_fk_schiffseffekt (mods_id), add constraint schiffsmodul_fk_schiffseffekt foreign key (mods_id) references schiffstyp_modifikation (id);
alter table schiffsmodul_slots add index schiffsmodul_slots_fk_schiffsmodul (Schiffsmodul_id), add constraint schiffsmodul_slots_fk_schiffsmodul foreign key (Schiffsmodul_id) references items (id);
ALTER TABLE schiffswaffenkonfiguration CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci;
alter table schiffswaffenkonfiguration add index weapon_changeset_fk_weapon (waffe_id), add constraint weapon_changeset_fk_weapon foreign key (waffe_id) references weapon (id);
alter table schiffswaffenkonfiguration add index schiffstyp_modifikation_waffen_fk_schiffstyp_modifikation (schiffstyp_modifikation_id), add constraint schiffstyp_modifikation_waffen_fk_schiffstyp_modifikation foreign key (schiffstyp_modifikation_id) references schiffstyp_modifikation (id);
alter table schiffstyp_modifikation add index schiffstypmodifikation_fk_schiffstyp (oneWayWerft_id), add constraint schiffstypmodifikation_fk_schiffstyp foreign key (oneWayWerft_id) references ship_types (id);

create table schiffstyp_modifikation_flags (SchiffstypModifikation_id bigint not null, flags integer) ENGINE=InnoDB;
alter table schiffstyp_modifikation_flags add index schiffstypmodifikation_flags_fk_schiffstypmodifikation (SchiffstypModifikation_id), add constraint schiffstypmodifikation_flags_fk_schiffstypmodifikation foreign key (SchiffstypModifikation_id) references schiffstyp_modifikation (id);

alter table items add column eff longtext;
update items set eff = effect;
update items set eff = replace(eff, 'module:', '') where typ='Schiffsmodul';

insert into schiffsmodul_slots select id, substring(eff, 1, LOCATE(';', eff)-1) from items where typ='Schiffsmodul' and eff like "%;%&%";
update items set eff = substring(eff, LOCATE(';', eff)+1) where typ='Schiffsmodul' and eff like "%;%&%";

insert into schiffsmodul_slots select id, substring(eff, 1, LOCATE(';', eff)-1) from items where typ='Schiffsmodul' and eff like "%;%&%";
update items set eff = substring(eff, LOCATE(';', eff)+1) where typ='Schiffsmodul' and eff like "%;%&%";

insert into schiffsmodul_slots select id, substring(eff, 1, LOCATE(';', eff)-1) from items where typ='Schiffsmodul' and eff like "%;%&%";
update items set eff = substring(eff, LOCATE(';', eff)+1) where typ='Schiffsmodul' and eff like "%;%&%";

insert into schiffsmodul_slots select id, substring(eff, 1, LOCATE('&', eff)-1) from items where typ='Schiffsmodul';
update items set eff = substring(eff, LOCATE('&', eff)+1) where typ='Schiffsmodul';

update items set set_id=substring(eff, 1, LOCATE('&', eff)-1) where typ='Schiffsmodul';
update items set set_id=null where set_id=0;
update items set eff = substring(eff, LOCATE('&', eff)+1) where typ='Schiffsmodul';

alter table schiffstyp_modifikation add column eff longtext after id;
insert into schiffstyp_modifikation select id,eff,0,0,null,0,0,0,0,0,0, 0,0,0,0,0,0,0,null, null,0,0,null,0,0,0,0,0,0,0,0,0,null,0,0,0,0 from items where typ='Schiffsmodul';
update items set mods_id=id where typ='Schiffsmodul';
update schiffstyp_modifikation set eff=CONCAT('|',eff,'|');
update schiffstyp_modifikation set nahrungcargo=substring(eff, LOCATE(',',eff, LOCATE('|nahrungcargo,', eff))+1, LOCATE('|', eff, LOCATE('|nahrungcargo,', eff)+1)-LOCATE(',',eff,LOCATE('|nahrungcargo,', eff)+1)-1) where eff like "%|nahrungcargo,%";
update schiffstyp_modifikation set nickname=substring(eff, LOCATE(',',eff, LOCATE('|nickname,', eff))+1, LOCATE('|', eff, LOCATE('|nickname,', eff)+1)-LOCATE(',',eff,LOCATE('|nickname,', eff)+1)-1) where eff like "%|nickname,%";
update schiffstyp_modifikation set picture=substring(eff, LOCATE(',',eff, LOCATE('|picture,', eff))+1, LOCATE('|', eff, LOCATE('|picture,', eff)+1)-LOCATE(',',eff,LOCATE('|picture,', eff)+1)-1) where eff like "%|picture,%";
update schiffstyp_modifikation set ru=substring(eff, LOCATE(',',eff, LOCATE('|ru,', eff))+1, LOCATE('|', eff, LOCATE('|ru,', eff)+1)-LOCATE(',',eff,LOCATE('|ru,', eff)+1)-1) where eff like "%|ru,%";
update schiffstyp_modifikation set rd=substring(eff, LOCATE(',',eff, LOCATE('|rd,', eff))+1, LOCATE('|', eff, LOCATE('|rd,', eff)+1)-LOCATE(',',eff,LOCATE('|rd,', eff)+1)-1) where eff like "%|rd,%";
update schiffstyp_modifikation set ra=substring(eff, LOCATE(',',eff, LOCATE('|ra,', eff))+1, LOCATE('|', eff, LOCATE('|ra,', eff)+1)-LOCATE(',',eff,LOCATE('|ra,', eff)+1)-1) where eff like "%|ra,%";
update schiffstyp_modifikation set rm=substring(eff, LOCATE(',',eff, LOCATE('|rm,', eff))+1, LOCATE('|', eff, LOCATE('|rm,', eff)+1)-LOCATE(',',eff,LOCATE('|rm,', eff)+1)-1) where eff like "%|rm,%";
update schiffstyp_modifikation set eps=substring(eff, LOCATE(',',eff, LOCATE('|eps,', eff))+1, LOCATE('|', eff, LOCATE('|eps,', eff)+1)-LOCATE(',',eff,LOCATE('|eps,', eff)+1)-1) where eff like "%|eps,%";
update schiffstyp_modifikation set cost=substring(eff, LOCATE(',',eff, LOCATE('|cost,', eff))+1, LOCATE('|', eff, LOCATE('|cost,', eff)+1)-LOCATE(',',eff,LOCATE('|cost,', eff)+1)-1) where eff like "%|cost,%";
update schiffstyp_modifikation set hull=substring(eff, LOCATE(',',eff, LOCATE('|hull,', eff))+1, LOCATE('|', eff, LOCATE('|hull,', eff)+1)-LOCATE(',',eff,LOCATE('|hull,', eff)+1)-1) where eff like "%|hull,%";
update schiffstyp_modifikation set panzerung=substring(eff, LOCATE(',',eff, LOCATE('|panzerung,', eff))+1, LOCATE('|', eff, LOCATE('|panzerung,', eff)+1)-LOCATE(',',eff,LOCATE('|panzerung,', eff)+1)-1) where eff like "%|panzerung,%";
update schiffstyp_modifikation set ablativeArmor=substring(eff, LOCATE(',',eff, LOCATE('|ablativearmor,', eff))+1, LOCATE('|', eff, LOCATE('|ablativearmor,', eff)+1)-LOCATE(',',eff,LOCATE('|ablativearmor,', eff)+1)-1) where eff like "%|ablativearmor,%";
update schiffstyp_modifikation set cargo=substring(eff, LOCATE(',',eff, LOCATE('|cargo,', eff))+1, LOCATE('|', eff, LOCATE('|cargo,', eff)+1)-LOCATE(',',eff,LOCATE('|cargo,', eff)+1)-1) where eff like "%|cargo,%";
update schiffstyp_modifikation set heat=substring(eff, LOCATE(',',eff, LOCATE('|heat,', eff))+1, LOCATE('|', eff, LOCATE('|heat,', eff)+1)-LOCATE(',',eff,LOCATE('|heat,', eff)+1)-1) where eff like "%|heat,%";
update schiffstyp_modifikation set crew=substring(eff, LOCATE(',',eff, LOCATE('|crew,', eff))+1, LOCATE('|', eff, LOCATE('|crew,', eff)+1)-LOCATE(',',eff,LOCATE('|crew,', eff)+1)-1) where eff like "%|crew,%";
update schiffstyp_modifikation set maxunitsize=substring(eff, LOCATE(',',eff, LOCATE('|maxunitsize,', eff))+1, LOCATE('|', eff, LOCATE('|maxunitsize,', eff)+1)-LOCATE(',',eff,LOCATE('|maxunitsize,', eff)+1)-1) where eff like "%|maxunitsize,%";
update schiffstyp_modifikation set unitspace=substring(eff, LOCATE(',',eff, LOCATE('|unitspace,', eff))+1, LOCATE('|', eff, LOCATE('|unitspace,', eff)+1)-LOCATE(',',eff,LOCATE('|unitspace,', eff)+1)-1) where eff like "%|unitspace,%";
update schiffstyp_modifikation set torpedoDef=substring(eff, LOCATE(',',eff, LOCATE('|torpdeff,', eff))+1, LOCATE('|', eff, LOCATE('|torpdeff,', eff)+1)-LOCATE(',',eff,LOCATE('|torpdeff,', eff)+1)-1) where eff like "%|torpdeff,%";
update schiffstyp_modifikation set shields=substring(eff, LOCATE(',',eff, LOCATE('|shields,', eff))+1, LOCATE('|', eff, LOCATE('|shields,', eff)+1)-LOCATE(',',eff,LOCATE('|shields,', eff)+1)-1) where eff like "%|shields,%";
update schiffstyp_modifikation set size=substring(eff, LOCATE(',',eff, LOCATE('|size,', eff))+1, LOCATE('|', eff, LOCATE('|size,', eff)+1)-LOCATE(',',eff,LOCATE('|size,', eff)+1)-1) where eff like "%|size,%";
update schiffstyp_modifikation set jDocks=substring(eff, LOCATE(',',eff, LOCATE('|jdocks,', eff))+1, LOCATE('|', eff, LOCATE('|jdocks,', eff)+1)-LOCATE(',',eff,LOCATE('|jdocks,', eff)+1)-1) where eff like "%|jdocks,%";
update schiffstyp_modifikation set aDocks=substring(eff, LOCATE(',',eff, LOCATE('|adocks,', eff))+1, LOCATE('|', eff, LOCATE('|adocks,', eff)+1)-LOCATE(',',eff,LOCATE('|adocks,', eff)+1)-1) where eff like "%|adocks,%";
update schiffstyp_modifikation set sensorRange=substring(eff, LOCATE(',',eff, LOCATE('|sensorrange,', eff))+1, LOCATE('|', eff, LOCATE('|sensorrange,', eff)+1)-LOCATE(',',eff,LOCATE('|sensorrange,', eff)+1)-1) where eff like "%|sensorrange,%";
update schiffstyp_modifikation set hydro=substring(eff, LOCATE(',',eff, LOCATE('|hydro,', eff))+1, LOCATE('|', eff, LOCATE('|hydro,', eff)+1)-LOCATE(',',eff,LOCATE('|hydro,', eff)+1)-1) where eff like "%|hydro,%";
update schiffstyp_modifikation set deutFactor=substring(eff, LOCATE(',',eff, LOCATE('|deutfactor,', eff))+1, LOCATE('|', eff, LOCATE('|deutfactor,', eff)+1)-LOCATE(',',eff,LOCATE('|deutfactor,', eff)+1)-1) where eff like "%|deutfactor,%";
update schiffstyp_modifikation set reCost=substring(eff, LOCATE(',',eff, LOCATE('|recost,', eff))+1, LOCATE('|', eff, LOCATE('|recost,', eff)+1)-LOCATE(',',eff,LOCATE('|recost,', eff)+1)-1) where eff like "%|recost,%";
update schiffstyp_modifikation set werft=substring(eff, LOCATE(',',eff, LOCATE('|werftslots,', eff))+1, LOCATE('|', eff, LOCATE('|werftslots,', eff)+1)-LOCATE(',',eff,LOCATE('|werftslots,', eff)+1)-1) where eff like "%|werftslots,%";
update schiffstyp_modifikation set oneWayWerft_id=substring(eff, LOCATE(',',eff, LOCATE('|onewaywerft,', eff))+1, LOCATE('|', eff, LOCATE('|onewaywerft,', eff)+1)-LOCATE(',',eff,LOCATE('|onewaywerft,', eff)+1)-1) where eff like "%|onewaywerft,%";
update schiffstyp_modifikation set srs=CASE substring(eff, LOCATE(',',eff, LOCATE('|srs,', eff))+1, LOCATE('|', eff, LOCATE('|srs,', eff)+1)-LOCATE(',',eff,LOCATE('|srs,', eff)+1)-1) WHEN 'true' THEN TRUE ELSE FALSE END where eff like "%|srs,%";
update schiffstyp_modifikation set scanCost=substring(eff, LOCATE(',',eff, LOCATE('|scancost,', eff))+1, LOCATE('|', eff, LOCATE('|scancost,', eff)+1)-LOCATE(',',eff,LOCATE('|scancost,', eff)+1)-1) where eff like "%|scancost,%";
update schiffstyp_modifikation set pickingCost=substring(eff, LOCATE(',',eff, LOCATE('|pickingcost,', eff))+1, LOCATE('|', eff, LOCATE('|pickingcost,', eff)+1)-LOCATE(',',eff,LOCATE('|pickingcost,', eff)+1)-1) where eff like "%|pickingcost,%";
update schiffstyp_modifikation set minCrew=substring(eff, LOCATE(',',eff, LOCATE('|mincrew,', eff))+1, LOCATE('|', eff, LOCATE('|mincrew,', eff)+1)-LOCATE(',',eff,LOCATE('|mincrew,', eff)+1)-1) where eff like "%|mincrew,%";
update schiffstyp_modifikation set lostInEmpChance=substring(eff, LOCATE(',',eff, LOCATE('|lostinempchance,', eff))+1, LOCATE('|', eff, LOCATE('|lostinempchance,', eff)+1)-LOCATE(',',eff,LOCATE('|lostinempchance,', eff)+1)-1) where eff like "%|lostinempchance,%";
update schiffstyp_modifikation set bounty=substring(eff, LOCATE(',',eff, LOCATE('|bounty,', eff))+1, LOCATE('|', eff, LOCATE('|bounty,', eff)+1)-LOCATE(',',eff,LOCATE('|bounty,', eff)+1)-1) where eff like "%|bounty,%";

alter table schiffstyp_modifikation add column eflag varchar(255), add column eweapon varchar(255);
update schiffstyp_modifikation set eflag=substring(eff, LOCATE(',',eff, LOCATE('|flags,', eff))+1, LOCATE('|', eff, LOCATE('|flags,', eff)+1)-LOCATE(',',eff,LOCATE('|flags,', eff)+1)-1) where eff like "%|flags,%";
update schiffstyp_modifikation set eweapon=substring(eff, LOCATE(',',eff, LOCATE('|weapons,', eff))+1, LOCATE('|', eff, LOCATE('|weapons,', eff)+1)-LOCATE(',',eff,LOCATE('|weapons,', eff)+1)-1) where eff like "%|weapons,%";

insert into schiffstyp_modifikation_flags select id,0 from schiffstyp_modifikation where eflag like "%jaeger%";
insert into schiffstyp_modifikation_flags select id,1 from schiffstyp_modifikation where eflag like "%zerstoererpanzerung%";
insert into schiffstyp_modifikation_flags select id,2 from schiffstyp_modifikation where eflag like "%colonizer%";
insert into schiffstyp_modifikation_flags select id,3 from schiffstyp_modifikation where eflag like "%abfangen%";
insert into schiffstyp_modifikation_flags select id,4 from schiffstyp_modifikation where eflag like "%nicht_kaperbar%";
insert into schiffstyp_modifikation_flags select id,5 from schiffstyp_modifikation where eflag like "%instabil%";
insert into schiffstyp_modifikation_flags select id,6 from schiffstyp_modifikation where eflag like "%sehr_klein%";
insert into schiffstyp_modifikation_flags select id,7 from schiffstyp_modifikation where eflag like "%kein_transfer%";
insert into schiffstyp_modifikation_flags select id,8 from schiffstyp_modifikation where eflag like "%srs_awac%";
insert into schiffstyp_modifikation_flags select id,9 from schiffstyp_modifikation where eflag like "%srs_ext_awac%";
insert into schiffstyp_modifikation_flags select id,10 from schiffstyp_modifikation where eflag like "%jumpdrive_shivan%";
insert into schiffstyp_modifikation_flags select id,11 from schiffstyp_modifikation where eflag like "%instant_battle_enter%";
insert into schiffstyp_modifikation_flags select id,12 from schiffstyp_modifikation where eflag like "%nicht_pluenderbar%";
insert into schiffstyp_modifikation_flags select id,13 from schiffstyp_modifikation where eflag like "%god_mode%";
insert into schiffstyp_modifikation_flags select id,14 from schiffstyp_modifikation where eflag like "%drohnen_controller%";
insert into schiffstyp_modifikation_flags select id,15 from schiffstyp_modifikation where eflag like "%drohne%";
insert into schiffstyp_modifikation_flags select id,16 from schiffstyp_modifikation where eflag like "%secondrow%";
insert into schiffstyp_modifikation_flags select id,17 from schiffstyp_modifikation where eflag like "%offitransport%";
insert into schiffstyp_modifikation_flags select id,18 from schiffstyp_modifikation where eflag like "%werftkomplex%";
insert into schiffstyp_modifikation_flags select id,19 from schiffstyp_modifikation where eflag like "%tradepost%";
insert into schiffstyp_modifikation_flags select id,20 from schiffstyp_modifikation where eflag like "%nosuicide%";
insert into schiffstyp_modifikation_flags select id,21 from schiffstyp_modifikation where eflag like "%versorger%";

insert into schiffswaffenkonfiguration select null,0,0,0,0,SUBSTRING(eweapon,1,LOCATE('/',eweapon)-1),id from schiffstyp_modifikation where eweapon is not null;
update schiffstyp_modifikation set eweapon=SUBSTRING(eweapon,LOCATE('/',eweapon)+1) where eweapon is not null;
update schiffswaffenkonfiguration swk set swk.anzahl=(SELECT SUBSTRING(eweapon,1,LOCATE('/',eweapon)-1) FROM schiffstyp_modifikation stm where stm.id=swk.schiffstyp_modifikation_id);
update schiffstyp_modifikation set eweapon=SUBSTRING(eweapon,LOCATE('/',eweapon)+1) where eweapon is not null;
update schiffswaffenkonfiguration swk set swk.hitze=(SELECT eweapon FROM schiffstyp_modifikation stm where stm.id=swk.schiffstyp_modifikation_id);

alter table items drop column eff;
alter table schiffstyp_modifikation drop column eff, drop column eflag, drop column eweapon;