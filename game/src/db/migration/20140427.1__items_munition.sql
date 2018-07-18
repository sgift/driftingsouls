alter table items add column munitionsdefinition_id integer;
update items set munitionsdefinition_id=SUBSTRING(effect,LENGTH("ammo:")+1) WHERE typ='Munition';

alter table items add index items_fk_munitionsdefinition (munitionsdefinition_id), add constraint items_fk_munitionsdefinition foreign key (munitionsdefinition_id) references ammo (id);

alter table ammo drop column itemid;