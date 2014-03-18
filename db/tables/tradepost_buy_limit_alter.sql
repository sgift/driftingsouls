alter table tradepost_buy_limit add index tradepost_buy_limit_fk_ships (shipid), add constraint tradepost_buy_limit_fk_ships foreign key (shipid) references ships (id);
