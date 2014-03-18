alter table jumps add index jumps_fk_ships (shipid), add constraint jumps_fk_ships foreign key (shipid) references ships (id);
