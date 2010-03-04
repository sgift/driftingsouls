CREATE TABLE `cargo_entries_units` (
  `type` int(11) NOT NULL,
  `destid` int(11) NOT NULL,
  `unittype` int(11) NOT NULL,
  `amount` int(11) NOT NULL,
  PRIMARY KEY  (`type`,`destid`,`unittype`)
)