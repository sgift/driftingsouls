alter table orders change user user_id integer;
alter table orders add index orders_fk_user (user_id), add constraint orders_fk_user foreign key (user_id) references users (id);

alter table orders add column shipType_id integer;
update orders set shipType_id=type where ordertype='ship';
alter table orders add index order_ship_fk_ship_type (shipType_id), add constraint order_ship_fk_ship_type foreign key (shipType_id) references ship_types (id);