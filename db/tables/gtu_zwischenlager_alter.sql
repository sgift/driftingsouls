ALTER TABLE gtu_zwischenlager ADD CONSTRAINT gtu_zwischenlager_fk_ships FOREIGN KEY (posten) REFERENCES ships(id);
ALTER TABLE gtu_zwischenlager ADD CONSTRAINT gtu_zwischenlager_fk_users1 FOREIGN KEY (user1) REFERENCES users(id);
ALTER TABLE gtu_zwischenlager ADD CONSTRAINT gtu_zwischenlager_fk_users2 FOREIGN KEY (user2) REFERENCES users(id);

create index posten on gtu_zwischenlager (posten, user1, user2);