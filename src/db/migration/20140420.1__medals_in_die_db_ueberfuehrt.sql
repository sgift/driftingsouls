create table medal (id integer not null auto_increment, adminOnly boolean not null, image varchar(255), imageSmall varchar(255), name varchar(255), primary key (id)) ENGINE=InnoDB;

INSERT INTO `medal` VALUES (1,0,'data/interface/medals/tapferkeit1.png','data/interface/medals/tapferkeit1_s.png','Orden der besonderen Tapferkeit');
INSERT INTO `medal` VALUES (2,0,'data/interface/medals/tapferkeit1.png','data/interface/medals/tapferkeit1_s.png','Orden der heldenhaften Tapferkeit');
INSERT INTO `medal` VALUES (3,0,'data/interface/medals/gcp1.png','data/interface/medals/gcp1_s.png','Orden des Commonwealth');
INSERT INTO `medal` VALUES (6,1,'data/interface/medals/ds2team.png','data/interface/medals/ds2team_s.png','Drifting Souls 2 Team');
INSERT INTO `medal` VALUES (7,1,'data/interface/medals/dssponsoring.png','data/interface/medals/dssponsoring_s.png','Sponsoring DS');
INSERT INTO `medal` VALUES (8,0,'data/interface/medals/hol_1.png','data/interface/medals/hol_1_s.png','HoL Einsatzmedaille');
INSERT INTO `medal` VALUES (9,0,'data/interface/medals/gtu_1.png','data/interface/medals/gtu_1_s.png','GTU Verdienstorden');
INSERT INTO `medal` VALUES (10,0,'data/interface/medals/gtu_1A.png','data/interface/medals/gtu_1A_s.png','GTU besonderer Verdienstorden');
INSERT INTO `medal` VALUES (11,0,'data/interface/medals/NTF.png','data/interface/medals/NTF_s.png','Einsatz gegen NTF');
INSERT INTO `medal` VALUES (12,0,'data/interface/medals/JF.png','data/interface/medals/JF_s.png','Joined Forces');
INSERT INTO `medal` VALUES (13,0,'data/interface/medals/tapferkeit1.png','data/interface/medals/tapferkeit1_s.png','Orden der Tapferkeit');

update users set medals=replace(medals,';0;',';13;') where medals like "%;0;%";
update users set medals=replace(medals,'0;','13;') where medals like "0;%";
update users set medals=replace(medals,';0',';13') where medals like "%;0";
update users set medals='13' where medals='0';