ALTER TABLE werft_queues ADD CONSTRAINT werft_queues_fk_werften FOREIGN KEY (werft) REFERENCES werften(id);
ALTER TABLE werft_queues ADD CONSTRAINT werft_queues_fk_ship_types FOREIGN KEY (building) REFERENCES ship_types(id);
