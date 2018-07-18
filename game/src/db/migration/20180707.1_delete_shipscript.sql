alter table ships drop foreign key ships_fk_ship_script_data;
alter table ships drop column scriptData_id;
drop table ship_script_data;