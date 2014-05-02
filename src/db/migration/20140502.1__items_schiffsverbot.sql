alter table items add column schiffstyp_id integer;
alter table items add index schiffsverbot_fk_schiffstyp (schiffstyp_id), add constraint schiffsverbot_fk_schiffstyp foreign key (schiffstyp_id) references ship_types (id);

update items set schiffstyp_id = substring(effect, LENGTH("disable-ship:")+1, LOCATE("&", effect)-LENGTH("disable-ship:")-1) where typ='Schiffsverbot';
update items set allianzEffekt = CASE substring(effect, LOCATE('&',effect)+1) WHEN 'true' THEN TRUE ELSE FALSE END where typ='Schiffsverbot';