CREATE TABLE `stats_user_cargo` (
  `user_id` int(11) NOT NULL default '0',
  `cargo` text NOT NULL,
  PRIMARY KEY  (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Die User-Cargo-Stats'; 

ALTER TABLE stats_user_cargo ADD CONSTRAINT stats_user_cargo_fk_user_id FOREIGN KEY (user_id) REFERENCES users(id);