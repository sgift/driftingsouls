CREATE TABLE `orders` (
  ordertype VARCHAR(32) NOT NULL,
  `id` int(11) NOT NULL auto_increment,
  `type` int(11) NOT NULL default '0',
  `tick` int(11) NOT NULL default '0',
  `user` int(11) NOT NULL default '0',
  `version` int(10) unsigned not null default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
