alter table kaserne_queues add index kaserne_queues_fk_unittype (unitid), add constraint kaserne_queues_fk_unittype foreign key (unitid) references unit_types (id);
alter table kaserne_queues add index kaserne_queues_fk_kaserne (kaserne), add constraint kaserne_queues_fk_kaserne foreign key (kaserne) references kaserne (id);
