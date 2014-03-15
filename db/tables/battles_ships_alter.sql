alter table battles_ships add index FK56245F3364F29ED2 (shipid), add constraint FK56245F3364F29ED2 foreign key (shipid) references ships (id);
alter table battles_ships add index battles_ships_fk_battles (battleid), add constraint battles_ships_fk_battles foreign key (battleid) references battles(id);
