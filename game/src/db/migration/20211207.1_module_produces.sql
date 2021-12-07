alter table ship_types add column produces longtext NOT NULL;
alter table ships_modules add column produces longtext NOT NULL;
alter table schiffstyp_modifikation add column produces longtext NOT NULL;

update ship_types set produces = '';
update ships_modules set produces = '';
update schiffstyp_modifikation set produces = '';