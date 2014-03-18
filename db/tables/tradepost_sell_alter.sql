alter table tradepost_sell add index tradepost_sell_fk_ships (shipid), add constraint tradepost_sell_fk_ships foreign key (shipid) references ships (id);
