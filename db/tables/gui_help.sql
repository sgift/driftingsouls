CREATE TABLE `gui_help` (
  `page` varchar(30) NOT NULL,
  `text` text,
  PRIMARY KEY  (`page`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

INSERT INTO `gui_help` (`page`, `text`) VALUES ('ueber', 'Test [b]Test[/b] [color=red]Test[/color]');