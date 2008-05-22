ALTER TABLE factions_shop_orders ADD CONSTRAINT factions_shop_orders_fk_factions_shop_entries FOREIGN KEY (shopentry_id) REFERENCES factions_shop_entries(id);
ALTER TABLE factions_shop_orders ADD CONSTRAINT factions_shop_orders_fk_users FOREIGN KEY (user_id) REFERENCES users(id);
