CREATE TABLE `user_values` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `name` varchar(60) NOT NULL default '',
  `value` varchar(40) NOT NULL default '',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`),
  KEY `id` (`user_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

ALTER TABLE user_values ADD CONSTRAINT user_values_fk_users FOREIGN KEY (`user_id`) REFERENCES users(id);

INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (607, 0, 'GAMEPLAY/bases/maxtiles', '500');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (20, 0, 'GTU_AUCTION_USER_COST', '10');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (2, 0, 'PIRATEN_Ansehen', '15');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (973, 0, 'PMS/signature', '');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (957, 0, 'TBLORDER/admin/show_cmdline', '0');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (6, 0, 'TBLORDER/basen/order', 'name');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (7, 0, 'TBLORDER/basen/order_mode', '0');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (8, 0, 'TBLORDER/basen/showcargo', '1');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (959, 0, 'TBLORDER/clients/jstarmap/bufferedoutput', '0');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (961, 0, 'TBLORDER/factions/konto_maxtype', '2');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (15, 0, 'TBLORDER/map/height', '600');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (14, 0, 'TBLORDER/map/width', '600');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (31, 0, 'TBLORDER/pms/forward', '0');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (12, 0, 'TBLORDER/schiff/sensororder', 'id');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (13, 0, 'TBLORDER/schiff/tooltips', '1');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (16, 0, 'TBLORDER/schiff/wrapfactor', '1');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (9, 0, 'TBLORDER/schiffe/mode', 'carg');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (10, 0, 'TBLORDER/schiffe/order', 'id');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (11, 0, 'TBLORDER/schiffe/showjaeger', '0');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (3, 0, 'TBLORDER/uebersicht/box', 'bookmarks');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (4, 0, 'TBLORDER/uebersicht/inttutorial', '5');
INSERT INTO `user_values` (`id`, `user_id`, `name`, `value`) VALUES (974, 0, 'PMS/signature', '');
