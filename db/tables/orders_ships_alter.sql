alter table orders_ships add index orders_ships_fk_shiptypes (type), add constraint orders_ships_fk_shiptypes foreign key (type) references ship_types (id);
