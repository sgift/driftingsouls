alter table werft_queues add index werft_queues_fk_ship_types (building), add constraint werft_queues_fk_ship_types foreign key (building) references ship_types (id);
alter table werft_queues add index werft_queues_fk_werften (werft), add constraint werft_queues_fk_werften foreign key (werft) references werften (id);
