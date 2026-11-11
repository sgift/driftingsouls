CREATE TABLE `discord_configuration` (
  `id` int NOT NULL,
  `active` tinyint(1) NOT NULL,
  `bot_token` varchar(255) DEFAULT NULL,
  `client_id` varchar(32) DEFAULT NULL,
  `client_secret` varchar(255) DEFAULT NULL,
  `redirect_uri` varchar(500) DEFAULT NULL,
  `guild_id` varchar(32) DEFAULT NULL,
  `invite_url` varchar(255) DEFAULT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

INSERT INTO `discord_configuration` (`id`, `active`, `guild_id`, `invite_url`, `version`)
VALUES (1, 0, '471610776126226432', 'https://discord.gg/cpxxAGy', 0);

-- Freigaben gelten fuer die Sichtbarkeit, die ein Kanal in Discord beim Freigeben hatte
-- (approved_signature). Weicht die zuletzt gesehene (discord_signature) ab, ist der Kanal gesperrt.
CREATE TABLE `discord_channel` (
  `id` int NOT NULL AUTO_INCREMENT,
  `guild_id` varchar(32) NOT NULL,
  `channel_id` varchar(32) NOT NULL,
  `name` varchar(100) NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `position` int NOT NULL,
  `read_access` varchar(20) NOT NULL,
  `write_access` varchar(20) NOT NULL,
  `default_channel` tinyint(1) NOT NULL,
  `bot_notice` varchar(255) DEFAULT NULL,
  `visible_to_everyone` tinyint(1) NOT NULL,
  `discord_signature` varchar(64) DEFAULT NULL,
  `approved_signature` varchar(64) DEFAULT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `discord_channel_guild_channel` (`guild_id`, `channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- Aus dem Spiel gesendete Nachrichten: Webhook-Nachrichten verraten nicht, welcher Account sie
-- geschrieben hat. Damit lassen sich z.B. die Rollenfarben auch fuer sie anzeigen. Eintraege werden
-- nach 30 Tagen, beim Trennen der Verknuepfung und beim Loeschen des Spielers entfernt.
CREATE TABLE `discord_sent_message` (
  `message_id` varchar(32) NOT NULL,
  `discord_user_id` varchar(32) NOT NULL,
  `user_id` int NOT NULL,
  `sent_at` datetime NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `discord_sent_message_user` (`user_id`),
  KEY `discord_sent_message_sent_at` (`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
