alter table orders_ships change column rasse rasse_id integer;
alter table orders_ships add index orderable_ship_fk_rasse (rasse_id), add constraint orderable_ship_fk_rasse foreign key (rasse_id) references rasse (id);