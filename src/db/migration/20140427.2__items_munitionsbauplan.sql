INSERT INTO `items_build` (`id`, `buildcosts`, `buildingid`, `dauer`, `name`, `produce`, `res1_id`, `res2_id`, `res3_id`) VALUES (20,'0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,20|20|0|0;21|5|0|0;17|20|0|0;23|5|0|0;19|2|0|0;22|10|0|0','22',1.00000,'NTF EM-Plasma','0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,168|1|0|0',1,NULL,NULL);

alter table items add column allianzEffekt boolean;
alter table items add column fabrikeintrag_id integer;
alter table items add index munitionsbauplan_fk_fabrikeintrag (fabrikeintrag_id), add constraint munitionsbauplan_fk_fabrikeintrag foreign key (fabrikeintrag_id) references items_build (id);

update items set fabrikeintrag_id = substring(effect, LENGTH("draft-ammo:")+1, LOCATE("&", effect)-LENGTH("draft-ammo:")-1) where typ='Munitionsbauplan';
update items set allianzEffekt = CASE substring(effect, LOCATE('&',effect)+1) WHEN 'true' THEN TRUE ELSE FALSE END where typ='Munitionsbauplan';