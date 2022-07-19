DROP INDEX ship_owner ON ships;
CREATE INDEX ship_owner ON ships (owner);