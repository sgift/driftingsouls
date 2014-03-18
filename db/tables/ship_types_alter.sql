alter table ship_types add index ship_types_fk_ship_types (ow_werft), add constraint ship_types_fk_ship_types foreign key (ow_werft) references ship_types (id);
