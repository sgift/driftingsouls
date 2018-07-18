alter table base_types add column largeImage varchar(255);
alter table base_types add column smallImage varchar(255);
alter table base_types add column starmapImage varchar(255);

update base_types SET largeImage=CONCAT('data/starmap/kolonie',id,'_srs.png'),smallImage=CONCAT('data/starmap/kolonie',id,'_lrs/kolonie',id,'_lrs.png'),starmapImage=CONCAT('data/starmap/kolonie',id,'_starmap.png');