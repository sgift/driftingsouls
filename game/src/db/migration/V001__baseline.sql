-- MySQL dump 10.13  Distrib 8.0.29, for Linux (x86_64)
--
-- Host: localhost    Database: ds
-- ------------------------------------------------------
-- Server version	8.0.29

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `academy`
--

DROP TABLE IF EXISTS `academy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academy` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `train` tinyint(1) NOT NULL,
                           `version` int NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32005 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `academy_queue_entry`
--

DROP TABLE IF EXISTS `academy_queue_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `academy_queue_entry` (
                                       `id` int NOT NULL AUTO_INCREMENT,
                                       `position` int NOT NULL,
                                       `remaining` int NOT NULL,
                                       `scheduled` tinyint(1) NOT NULL,
                                       `training` int NOT NULL,
                                       `trainingtype` int NOT NULL,
                                       `academy_id` int NOT NULL,
                                       PRIMARY KEY (`id`),
                                       KEY `academy_queues_fk_academy` (`academy_id`),
                                       CONSTRAINT `academy_queues_fk_academy` FOREIGN KEY (`academy_id`) REFERENCES `academy` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1147598 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ally`
--

DROP TABLE IF EXISTS `ally`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ally` (
                        `id` int NOT NULL AUTO_INCREMENT,
                        `allytag` varchar(255) NOT NULL,
                        `description` longtext NOT NULL,
                        `destroyedShips` int NOT NULL,
                        `founded` datetime NOT NULL,
                        `hp` longtext NOT NULL,
                        `items` longtext NOT NULL,
                        `lostBattles` smallint NOT NULL,
                        `lostShips` int NOT NULL,
                        `name` longtext NOT NULL,
                        `plainname` varchar(255) NOT NULL,
                        `pname` varchar(255) NOT NULL,
                        `showGtuBieter` tinyint NOT NULL,
                        `showastis` tinyint(1) NOT NULL,
                        `showlrs` tinyint NOT NULL,
                        `tick` int NOT NULL,
                        `version` int NOT NULL,
                        `wonBattles` smallint NOT NULL,
                        `president` int NOT NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `president` (`president`),
                        KEY `ally_fk_users` (`president`),
                        CONSTRAINT `ally_fk_users` FOREIGN KEY (`president`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=222 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ally_posten`
--

DROP TABLE IF EXISTS `ally_posten`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ally_posten` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `name` varchar(255) NOT NULL,
                               `version` int NOT NULL,
                               `ally` int NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `ally_posten_fk_ally` (`ally`),
                               CONSTRAINT `ally_posten_fk_ally` FOREIGN KEY (`ally`) REFERENCES `ally` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=861 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ally_rangdescriptors`
--

DROP TABLE IF EXISTS `ally_rangdescriptors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ally_rangdescriptors` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `customImg` varchar(255) COLLATE utf8_bin DEFAULT NULL,
                                        `name` varchar(255) COLLATE utf8_bin NOT NULL,
                                        `rang` int NOT NULL,
                                        `version` int NOT NULL,
                                        `ally_id` int NOT NULL,
                                        PRIMARY KEY (`id`),
                                        KEY `ally_rangdescriptors_fk_ally` (`ally_id`),
                                        CONSTRAINT `ally_rangdescriptors_fk_ally` FOREIGN KEY (`ally_id`) REFERENCES `ally` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ammo`
--

DROP TABLE IF EXISTS `ammo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ammo` (
                        `id` int NOT NULL AUTO_INCREMENT,
                        `areadamage` int NOT NULL,
                        `damage` int NOT NULL,
                        `destroyable` double NOT NULL,
                        `name` varchar(255) NOT NULL,
                        `shielddamage` int NOT NULL,
                        `shotspershot` int NOT NULL,
                        `smalltrefferws` int NOT NULL,
                        `subdamage` int NOT NULL,
                        `subws` int NOT NULL,
                        `torptrefferws` int NOT NULL,
                        `trefferws` int NOT NULL,
                        `type` varchar(255) NOT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ammo_flags`
--

DROP TABLE IF EXISTS `ammo_flags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ammo_flags` (
                              `Ammo_id` int NOT NULL,
                              `flags` int DEFAULT NULL,
                              KEY `ammo_flag_fk_ammo` (`Ammo_id`),
                              CONSTRAINT `ammo_flag_fk_ammo` FOREIGN KEY (`Ammo_id`) REFERENCES `ammo` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_types`
--

DROP TABLE IF EXISTS `base_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_types` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `cargo` int NOT NULL,
                              `energy` int NOT NULL,
                              `height` int NOT NULL,
                              `maxtiles` int NOT NULL,
                              `name` varchar(255) NOT NULL,
                              `size` int NOT NULL,
                              `spawnableress` longtext,
                              `terrain` longtext,
                              `width` int NOT NULL,
                              `largeImage` varchar(255) DEFAULT NULL,
                              `smallImage` varchar(255) DEFAULT NULL,
                              `starmapImage` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bases`
--

DROP TABLE IF EXISTS `bases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bases` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `active` longtext NOT NULL,
                         `arbeiter` int NOT NULL,
                         `autogtuacts` longtext NOT NULL,
                         `bebauung` longtext NOT NULL,
                         `bewohner` int NOT NULL,
                         `cargo` longtext NOT NULL,
                         `core_id` int DEFAULT NULL,
                         `coreactive` int NOT NULL,
                         `e` int NOT NULL,
                         `height` int NOT NULL,
                         `isfeeding` tinyint(1) NOT NULL,
                         `isloading` tinyint(1) NOT NULL,
                         `maxcargo` bigint NOT NULL,
                         `maxe` int NOT NULL,
                         `maxtiles` int NOT NULL,
                         `name` varchar(255) NOT NULL,
                         `size` int NOT NULL,
                         `spawnableress` longtext,
                         `spawnressavailable` longtext,
                         `star_system` int DEFAULT NULL,
                         `terrain` longtext NOT NULL,
                         `version` int NOT NULL,
                         `width` int NOT NULL,
                         `x` int NOT NULL,
                         `y` int NOT NULL,
                         `academy_id` int DEFAULT NULL,
                         `forschungszentrum_id` int DEFAULT NULL,
                         `klasse` int NOT NULL,
                         `owner` int NOT NULL,
                         `werft_id` int DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `coords` (`x`,`y`,`star_system`),
                         KEY `owner` (`owner`,`id`),
                         KEY `idx_feeding` (`isfeeding`),
                         KEY `bases_fk_academy` (`academy_id`),
                         KEY `bases_fk_fz` (`forschungszentrum_id`),
                         KEY `bases_fk_werften` (`werft_id`),
                         KEY `bases_fk_basetypes` (`klasse`),
                         KEY `bases_fk_users` (`owner`),
                         KEY `bases_fk_core` (`core_id`),
                         CONSTRAINT `bases_fk_academy` FOREIGN KEY (`academy_id`) REFERENCES `academy` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `bases_fk_basetypes` FOREIGN KEY (`klasse`) REFERENCES `base_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `bases_fk_core` FOREIGN KEY (`core_id`) REFERENCES `cores` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `bases_fk_fz` FOREIGN KEY (`forschungszentrum_id`) REFERENCES `fz` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `bases_fk_users` FOREIGN KEY (`owner`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `bases_fk_werften` FOREIGN KEY (`werft_id`) REFERENCES `werften` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=44114 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `battles`
--

DROP TABLE IF EXISTS `battles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `battles` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `ally1` int NOT NULL,
                           `ally2` int NOT NULL,
                           `blockcount` int NOT NULL,
                           `com1BETAK` tinyint(1) NOT NULL,
                           `com2BETAK` tinyint(1) NOT NULL,
                           `flags` int NOT NULL,
                           `lastaction` bigint NOT NULL,
                           `lastturn` bigint NOT NULL,
                           `ready1` tinyint(1) NOT NULL,
                           `ready2` tinyint(1) NOT NULL,
                           `star_system` int DEFAULT NULL,
                           `takeCommand1` int NOT NULL,
                           `takeCommand2` int NOT NULL,
                           `version` int NOT NULL,
                           `x` int NOT NULL,
                           `y` int NOT NULL,
                           `commander1` int NOT NULL,
                           `commander2` int NOT NULL,
                           `schlachtLog_id` bigint DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           KEY `battles_fk_users1` (`commander1`),
                           KEY `battles_fk_users2` (`commander2`),
                           KEY `battle_coords` (`x`,`y`,`star_system`),
                           KEY `battles_fk_schlachtlog` (`schlachtLog_id`),
                           CONSTRAINT `battles_fk_schlachtlog` FOREIGN KEY (`schlachtLog_id`) REFERENCES `schlacht_log` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT `battles_fk_users1` FOREIGN KEY (`commander1`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT `battles_fk_users2` FOREIGN KEY (`commander2`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `battles_ships`
--

DROP TABLE IF EXISTS `battles_ships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `battles_ships` (
                                 `shipid` int NOT NULL,
                                 `ablativeArmor` int NOT NULL,
                                 `action` int NOT NULL,
                                 `comm` int NOT NULL,
                                 `destroyer` int NOT NULL,
                                 `engine` int NOT NULL,
                                 `hull` int NOT NULL,
                                 `sensors` int NOT NULL,
                                 `shields` int NOT NULL,
                                 `side` int NOT NULL,
                                 `version` int NOT NULL,
                                 `weapons` int NOT NULL,
                                 `battleid` int NOT NULL,
                                 PRIMARY KEY (`shipid`),
                                 KEY `FK56245F3364F29ED2` (`shipid`),
                                 KEY `battles_ships_fk_battles` (`battleid`),
                                 CONSTRAINT `battles_ships_fk_battles` FOREIGN KEY (`battleid`) REFERENCES `battles` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                 CONSTRAINT `FK56245F3364F29ED2` FOREIGN KEY (`shipid`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `building_alternativebilder`
--

DROP TABLE IF EXISTS `building_alternativebilder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `building_alternativebilder` (
                                              `Building_id` int NOT NULL,
                                              `alternativeBilder` varchar(255) DEFAULT NULL,
                                              `alternativeBilder_KEY` int NOT NULL DEFAULT '0',
                                              PRIMARY KEY (`Building_id`,`alternativeBilder_KEY`),
                                              KEY `building_alternativebilder_fk_building` (`Building_id`),
                                              CONSTRAINT `building_alternativebilder_fk_building` FOREIGN KEY (`Building_id`) REFERENCES `buildings` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `buildings`
--

DROP TABLE IF EXISTS `buildings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `buildings` (
                             `id` int NOT NULL AUTO_INCREMENT,
                             `arbeiter` int NOT NULL,
                             `bewohner` int NOT NULL,
                             `buildcosts` varchar(255) NOT NULL,
                             `category` int NOT NULL,
                             `chanceress` varchar(255) DEFAULT NULL,
                             `consumes` varchar(255) NOT NULL,
                             `deakable` tinyint(1) NOT NULL,
                             `eprodu` int NOT NULL,
                             `ever` int NOT NULL,
                             `eps` int NOT NULL,
                             `module` varchar(255) NOT NULL,
                             `name` varchar(255) NOT NULL,
                             `perowner` int NOT NULL,
                             `perplanet` int NOT NULL,
                             `picture` varchar(255) NOT NULL,
                             `produces` varchar(255) NOT NULL,
                             `race` int NOT NULL,
                             `shutdown` tinyint(1) NOT NULL,
                             `techreq_id` int DEFAULT NULL,
                             `terrain` longtext,
                             `ucomplex` tinyint(1) NOT NULL,
                             PRIMARY KEY (`id`),
                             KEY `building_fk_forschung` (`techreq_id`),
                             KEY `building_category` (`category`),
                             CONSTRAINT `building_fk_forschung` FOREIGN KEY (`techreq_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1015 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cargo_entries_units`
--

DROP TABLE IF EXISTS `cargo_entries_units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cargo_entries_units` (
                                       `type` int NOT NULL,
                                       `id` bigint NOT NULL AUTO_INCREMENT,
                                       `amount` bigint NOT NULL,
                                       `unittype` int NOT NULL,
                                       `basis_id` int DEFAULT NULL,
                                       `schiff_id` int DEFAULT NULL,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `type` (`type`,`basis_id`,`schiff_id`,`unittype`),
                                       KEY `cargo_entries_units_fk_basis` (`basis_id`),
                                       KEY `cargo_entries_units_fk_schiff` (`schiff_id`),
                                       KEY `cargo_entries_units_fk_unittype` (`unittype`),
                                       CONSTRAINT `cargo_entries_units_fk_basis` FOREIGN KEY (`basis_id`) REFERENCES `bases` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                       CONSTRAINT `cargo_entries_units_fk_schiff` FOREIGN KEY (`schiff_id`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                       CONSTRAINT `cargo_entries_units_fk_unittype` FOREIGN KEY (`unittype`) REFERENCES `unit_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=50484 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config`
--

DROP TABLE IF EXISTS `config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config` (
                          `name` varchar(255) NOT NULL,
                          `value` longtext NOT NULL,
                          `version` int NOT NULL,
                          PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_felsbrocken`
--

DROP TABLE IF EXISTS `config_felsbrocken`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_felsbrocken` (
                                      `id` int NOT NULL AUTO_INCREMENT,
                                      `cargo` varchar(255) NOT NULL,
                                      `chance` int NOT NULL,
                                      `shiptype` int NOT NULL,
                                      `system_id` bigint NOT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `fk_config_felsbrocken_shiptype` (`shiptype`),
                                      KEY `fk_config_felsbrocken_system` (`system_id`),
                                      CONSTRAINT `fk_config_felsbrocken_shiptype` FOREIGN KEY (`shiptype`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                      CONSTRAINT `fk_config_felsbrocken_system` FOREIGN KEY (`system_id`) REFERENCES `config_felsbrocken_systems` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_felsbrocken_systems`
--

DROP TABLE IF EXISTS `config_felsbrocken_systems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_felsbrocken_systems` (
                                              `system_id` int NOT NULL,
                                              `count` int NOT NULL,
                                              `id` bigint NOT NULL AUTO_INCREMENT,
                                              `name` varchar(255) DEFAULT NULL,
                                              PRIMARY KEY (`id`),
                                              KEY `config_felsbrocken_systems_fk_system` (`system_id`),
                                              CONSTRAINT `config_felsbrocken_systems_fk_system` FOREIGN KEY (`system_id`) REFERENCES `systems` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cores`
--

DROP TABLE IF EXISTS `cores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cores` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `arbeiter` int NOT NULL,
                         `astitype` int NOT NULL,
                         `bewohner` int NOT NULL,
                         `buildcosts` varchar(255) NOT NULL,
                         `consumes` varchar(255) NOT NULL,
                         `eprodu` int NOT NULL,
                         `ever` int NOT NULL,
                         `eps` int NOT NULL,
                         `name` varchar(255) NOT NULL,
                         `produces` varchar(255) NOT NULL,
                         `shutdown` tinyint(1) NOT NULL,
                         `techreq_id` int DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `core_fk_forschung` (`techreq_id`),
                         KEY `core_fk_basetype` (`astitype`),
                         CONSTRAINT `core_fk_basetype` FOREIGN KEY (`astitype`) REFERENCES `base_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `core_fk_forschung` FOREIGN KEY (`techreq_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dynamic_content`
--

DROP TABLE IF EXISTS `dynamic_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dynamic_content` (
                                   `id` varchar(255) NOT NULL,
                                   `aenderungsdatum` datetime DEFAULT NULL,
                                   `anlagedatum` datetime DEFAULT NULL,
                                   `autor` varchar(255) DEFAULT NULL,
                                   `lizenz` varchar(255) DEFAULT NULL,
                                   `lizenzdetails` longtext,
                                   `quelle` varchar(255) DEFAULT NULL,
                                   `version` int NOT NULL,
                                   `hochgeladenDurch_id` int DEFAULT NULL,
                                   PRIMARY KEY (`id`),
                                   KEY `dynamic_content_fk_users` (`hochgeladenDurch_id`),
                                   CONSTRAINT `dynamic_content_fk_users` FOREIGN KEY (`hochgeladenDurch_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dynamic_jn_config`
--

DROP TABLE IF EXISTS `dynamic_jn_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dynamic_jn_config` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `maxDistanceToInitialStart` int NOT NULL,
                                     `maxLifetime` int NOT NULL,
                                     `maxNextMovementDelay` int NOT NULL,
                                     `minLifetime` int NOT NULL,
                                     `minNextMovementDelay` int NOT NULL,
                                     `maxDistanceToInitialTarget` int NOT NULL,
                                     `initialStartSystem` int NOT NULL,
                                     `initialStartX` int NOT NULL,
                                     `initialStartY` int NOT NULL,
                                     `initialTargetSystem` int NOT NULL,
                                     `initialTargetX` int NOT NULL,
                                     `initialTargetY` int NOT NULL,
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dynamic_jumpnode`
--

DROP TABLE IF EXISTS `dynamic_jumpnode`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dynamic_jumpnode` (
                                    `id` int NOT NULL AUTO_INCREMENT,
                                    `config_id` int NOT NULL,
                                    `initialTicksUntilMove` int NOT NULL,
                                    `remainingTicksUntilMove` int NOT NULL,
                                    `remainingLiveTime` int NOT NULL,
                                    `jumpnode_id` int DEFAULT NULL,
                                    PRIMARY KEY (`id`),
                                    KEY `dynamic_jn_fk_jumpnodes` (`jumpnode_id`),
                                    KEY `dynamic_jn_fk_dynamicjnconfig` (`config_id`),
                                    CONSTRAINT `dynamic_jn_fk_dynamicjnconfig` FOREIGN KEY (`config_id`) REFERENCES `dynamic_jn_config` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                    CONSTRAINT `dynamic_jn_fk_jumpnodes` FOREIGN KEY (`jumpnode_id`) REFERENCES `jumpnodes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `factions_angebote`
--

DROP TABLE IF EXISTS `factions_angebote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `factions_angebote` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `description` longtext NOT NULL,
                                     `faction` int NOT NULL,
                                     `image` varchar(255) NOT NULL,
                                     `title` varchar(255) NOT NULL,
                                     `version` int NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `factions_angebote_fk_user` (`faction`),
                                     CONSTRAINT `factions_angebote_fk_user` FOREIGN KEY (`faction`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `factions_shop_entries`
--

DROP TABLE IF EXISTS `factions_shop_entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `factions_shop_entries` (
                                         `id` int NOT NULL AUTO_INCREMENT,
                                         `availability` int NOT NULL,
                                         `faction_id` int NOT NULL,
                                         `lpKosten` bigint NOT NULL,
                                         `min_rank` int NOT NULL,
                                         `price` bigint NOT NULL,
                                         `resource` varchar(255) NOT NULL,
                                         `type` int NOT NULL,
                                         `version` int NOT NULL,
                                         PRIMARY KEY (`id`),
                                         KEY `factions_shop_entries_fk_users` (`faction_id`),
                                         CONSTRAINT `factions_shop_entries_fk_users` FOREIGN KEY (`faction_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1095 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `factions_shop_orders`
--

DROP TABLE IF EXISTS `factions_shop_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `factions_shop_orders` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `adddata` longtext,
                                        `count` int NOT NULL,
                                        `date` bigint NOT NULL,
                                        `lpKosten` bigint NOT NULL,
                                        `price` bigint NOT NULL,
                                        `status` int NOT NULL,
                                        `version` int NOT NULL,
                                        `shopentry_id` int NOT NULL,
                                        `user_id` int NOT NULL,
                                        PRIMARY KEY (`id`),
                                        KEY `factions_shop_orders_fk_factions_shop_entries` (`shopentry_id`),
                                        KEY `factions_shop_orders_fk_users` (`user_id`),
                                        CONSTRAINT `factions_shop_orders_fk_factions_shop_entries` FOREIGN KEY (`shopentry_id`) REFERENCES `factions_shop_entries` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                        CONSTRAINT `factions_shop_orders_fk_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=4057 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `factory`
--

DROP TABLE IF EXISTS `factory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `factory` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `buildingid` int NOT NULL,
                           `count` int NOT NULL,
                           `produces` longtext NOT NULL,
                           `version` int NOT NULL,
                           `col` int DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `col` (`col`,`buildingid`),
                           KEY `factory_fk_bases` (`col`),
                           CONSTRAINT `factory_fk_bases` FOREIGN KEY (`col`) REFERENCES `bases` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=6610 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forschungen`
--

DROP TABLE IF EXISTS `forschungen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `forschungen` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `costs` varchar(255) NOT NULL,
                               `description` longtext NOT NULL,
                               `flags` varchar(255) NOT NULL,
                               `image` varchar(255) DEFAULT NULL,
                               `name` varchar(255) NOT NULL,
                               `race` int NOT NULL,
                               `req1_id` int DEFAULT NULL,
                               `req2_id` int DEFAULT NULL,
                               `req3_id` int DEFAULT NULL,
                               `specializationCosts` int NOT NULL,
                               `time` int NOT NULL,
                               `visibility` int NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `forschung_fk_forschung3` (`req3_id`),
                               KEY `forschung_fk_forschung2` (`req2_id`),
                               KEY `forschung_fk_forschung1` (`req1_id`),
                               CONSTRAINT `forschung_fk_forschung1` FOREIGN KEY (`req1_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `forschung_fk_forschung2` FOREIGN KEY (`req2_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `forschung_fk_forschung3` FOREIGN KEY (`req3_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=925 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fraktion_aktions_meldung`
--

DROP TABLE IF EXISTS `fraktion_aktions_meldung`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fraktion_aktions_meldung` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `bearbeitetAm` datetime DEFAULT NULL,
                                            `gemeldetAm` datetime DEFAULT NULL,
                                            `meldungstext` longtext,
                                            `version` int NOT NULL,
                                            `fraktion_id` int DEFAULT NULL,
                                            `gemeldetVon_id` int DEFAULT NULL,
                                            PRIMARY KEY (`id`),
                                            KEY `fraktion_aktions_meldung_fk_users` (`gemeldetVon_id`),
                                            KEY `fraktion_aktions_meldung_fk_users2` (`fraktion_id`),
                                            CONSTRAINT `fraktion_aktions_meldung_fk_users` FOREIGN KEY (`gemeldetVon_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                            CONSTRAINT `fraktion_aktions_meldung_fk_users2` FOREIGN KEY (`fraktion_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1913 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fraktions_gui_eintrag`
--

DROP TABLE IF EXISTS `fraktions_gui_eintrag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fraktions_gui_eintrag` (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `text` longtext,
                                         `version` int NOT NULL,
                                         `user_id` int NOT NULL,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `user_id` (`user_id`),
                                         KEY `fraktionsguieintrag_fk_user` (`user_id`),
                                         CONSTRAINT `fraktionsguieintrag_fk_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fraktions_gui_eintrag_seiten`
--

DROP TABLE IF EXISTS `fraktions_gui_eintrag_seiten`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fraktions_gui_eintrag_seiten` (
                                                `FraktionsGuiEintrag_id` bigint NOT NULL,
                                                `seiten` varchar(255) DEFAULT NULL,
                                                KEY `fraktionsguieintrag_fk_fraktionsguieintrag_seiten` (`FraktionsGuiEintrag_id`),
                                                CONSTRAINT `fraktionsguieintrag_fk_fraktionsguieintrag_seiten` FOREIGN KEY (`FraktionsGuiEintrag_id`) REFERENCES `fraktions_gui_eintrag` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fz`
--

DROP TABLE IF EXISTS `fz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fz` (
                      `id` int NOT NULL AUTO_INCREMENT,
                      `dauer` int NOT NULL,
                      `type` int NOT NULL,
                      `version` int NOT NULL,
                      `forschung` int DEFAULT NULL,
                      PRIMARY KEY (`id`),
                      KEY `fz_fk_forschungen` (`forschung`),
                      CONSTRAINT `fz_fk_forschungen` FOREIGN KEY (`forschung`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=33596 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `global_sectortemplates`
--

DROP TABLE IF EXISTS `global_sectortemplates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `global_sectortemplates` (
                                          `id` varchar(255) NOT NULL,
                                          `h` int NOT NULL,
                                          `scriptid` int NOT NULL,
                                          `w` int NOT NULL,
                                          `x` int NOT NULL,
                                          `y` int NOT NULL,
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gtu_warenkurse`
--

DROP TABLE IF EXISTS `gtu_warenkurse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gtu_warenkurse` (
                                  `place` varchar(255) NOT NULL,
                                  `kurse` longtext NOT NULL,
                                  `name` varchar(255) NOT NULL,
                                  `version` int NOT NULL,
                                  PRIMARY KEY (`place`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gtu_zwischenlager`
--

DROP TABLE IF EXISTS `gtu_zwischenlager`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gtu_zwischenlager` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `cargo1` longtext NOT NULL,
                                     `cargo1need` longtext NOT NULL,
                                     `cargo2` longtext NOT NULL,
                                     `cargo2need` longtext NOT NULL,
                                     `version` int NOT NULL,
                                     `posten` int NOT NULL,
                                     `user1` int NOT NULL,
                                     `user2` int NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `posten` (`posten`,`user1`,`user2`),
                                     KEY `gtu_zwischenlager_fk_users1` (`user1`),
                                     KEY `gtu_zwischenlager_fk_users2` (`user2`),
                                     KEY `gtu_zwischenlager_fk_ships` (`posten`),
                                     CONSTRAINT `gtu_zwischenlager_fk_ships` FOREIGN KEY (`posten`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT `gtu_zwischenlager_fk_users1` FOREIGN KEY (`user1`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT `gtu_zwischenlager_fk_users2` FOREIGN KEY (`user2`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2699 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gui_help`
--

DROP TABLE IF EXISTS `gui_help`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gui_help` (
                            `page` varchar(255) NOT NULL,
                            `text` longtext,
                            PRIMARY KEY (`page`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `handel`
--

DROP TABLE IF EXISTS `handel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `handel` (
                          `id` int NOT NULL AUTO_INCREMENT,
                          `bietet` longtext NOT NULL,
                          `comm` longtext NOT NULL,
                          `sucht` longtext NOT NULL,
                          `time` bigint NOT NULL,
                          `version` int NOT NULL,
                          `who` int NOT NULL,
                          PRIMARY KEY (`id`),
                          KEY `handel_fk_users` (`who`),
                          CONSTRAINT `handel_fk_users` FOREIGN KEY (`who`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=9763 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hibernate_sequences`
--

DROP TABLE IF EXISTS `hibernate_sequences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hibernate_sequences` (
                                       `sequence_name` varchar(255) NOT NULL,
                                       `sequence_next_hi_value` bigint DEFAULT NULL,
                                       PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `inttutorial`
--

DROP TABLE IF EXISTS `inttutorial`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inttutorial` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `headimg` varchar(255) NOT NULL,
                               `reqBase` tinyint(1) NOT NULL,
                               `reqName` tinyint(1) NOT NULL,
                               `benoetigteSeite_id` int DEFAULT NULL,
                               `reqShip` tinyint(1) NOT NULL,
                               `text` longtext NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `inttutorial_fk_inttutorial` (`benoetigteSeite_id`),
                               CONSTRAINT `inttutorial_fk_inttutorial` FOREIGN KEY (`benoetigteSeite_id`) REFERENCES `inttutorial` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `items`
--

DROP TABLE IF EXISTS `items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items` (
                         `typ` varchar(255) NOT NULL,
                         `id` int NOT NULL,
                         `accesslevel` int NOT NULL,
                         `cargo` bigint NOT NULL,
                         `description` longtext,
                         `handel` tinyint(1) NOT NULL,
                         `isspawnable` tinyint(1) NOT NULL,
                         `largepicture` longtext,
                         `name` varchar(255) NOT NULL,
                         `picture` longtext,
                         `quality` varchar(255) DEFAULT NULL,
                         `unknownItem` tinyint(1) NOT NULL,
                         `munitionsdefinition_id` int DEFAULT NULL,
                         `allianzEffekt` tinyint(1) DEFAULT NULL,
                         `fabrikeintrag_id` int DEFAULT NULL,
                         `schiffstyp_id` int DEFAULT NULL,
                         `baukosten` longtext,
                         `crew` int DEFAULT NULL,
                         `dauer` int DEFAULT NULL,
                         `energiekosten` int DEFAULT NULL,
                         `flagschiff` tinyint(1) DEFAULT NULL,
                         `werftSlots` int DEFAULT NULL,
                         `schiffsbauplan_rasse_id` int DEFAULT NULL,
                         `schiffsbauplan_schiffstyp_id` int DEFAULT NULL,
                         `set_id` int DEFAULT NULL,
                         `mods_id` bigint DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `items_fk_munitionsdefinition` (`munitionsdefinition_id`),
                         KEY `munitionsbauplan_fk_fabrikeintrag` (`fabrikeintrag_id`),
                         KEY `schiffsverbot_fk_schiffstyp` (`schiffstyp_id`),
                         KEY `schiffsbauplan_fk_rasse` (`schiffsbauplan_rasse_id`),
                         KEY `schiffsbauplan_fk_schiffstyp` (`schiffsbauplan_schiffstyp_id`),
                         KEY `schiffsmodul_fk_schiffseffekt` (`mods_id`),
                         KEY `schiffsmodul_fk_schiffsmodulset` (`set_id`),
                         CONSTRAINT `items_fk_munitionsdefinition` FOREIGN KEY (`munitionsdefinition_id`) REFERENCES `ammo` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `munitionsbauplan_fk_fabrikeintrag` FOREIGN KEY (`fabrikeintrag_id`) REFERENCES `items_build` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `schiffsbauplan_fk_rasse` FOREIGN KEY (`schiffsbauplan_rasse_id`) REFERENCES `rasse` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `schiffsbauplan_fk_schiffstyp` FOREIGN KEY (`schiffsbauplan_schiffstyp_id`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `schiffsmodul_fk_schiffseffekt` FOREIGN KEY (`mods_id`) REFERENCES `schiffstyp_modifikation` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `schiffsmodul_fk_schiffsmodulset` FOREIGN KEY (`set_id`) REFERENCES `items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `schiffsverbot_fk_schiffstyp` FOREIGN KEY (`schiffstyp_id`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `items_build`
--

DROP TABLE IF EXISTS `items_build`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_build` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `buildcosts` varchar(255) NOT NULL,
                               `buildingid` varchar(255) NOT NULL,
                               `dauer` decimal(19,5) NOT NULL,
                               `name` varchar(255) NOT NULL,
                               `produce` varchar(255) NOT NULL,
                               `res1_id` int DEFAULT NULL,
                               `res2_id` int DEFAULT NULL,
                               `res3_id` int DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `factoryentry_fk_forschung2` (`res2_id`),
                               KEY `factoryentry_fk_forschung1` (`res1_id`),
                               KEY `factoryentry_fk_forschung3` (`res3_id`),
                               CONSTRAINT `factoryentry_fk_forschung1` FOREIGN KEY (`res1_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `factoryentry_fk_forschung2` FOREIGN KEY (`res2_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `factoryentry_fk_forschung3` FOREIGN KEY (`res3_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1170 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `items_schiffstyp_modifikation`
--

DROP TABLE IF EXISTS `items_schiffstyp_modifikation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_schiffstyp_modifikation` (
                                                 `items_id` int NOT NULL,
                                                 `setEffekte_id` bigint NOT NULL,
                                                 `setEffekte_KEY` int NOT NULL DEFAULT '0',
                                                 PRIMARY KEY (`items_id`,`setEffekte_KEY`),
                                                 KEY `schiffstypmodifikation_fk_schiffsmodulset` (`setEffekte_id`),
                                                 KEY `schiffsmodulset_fk_schiffstypmodifikation` (`items_id`),
                                                 CONSTRAINT `schiffsmodulset_fk_schiffstypmodifikation` FOREIGN KEY (`items_id`) REFERENCES `items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                                 CONSTRAINT `schiffstypmodifikation_fk_schiffsmodulset` FOREIGN KEY (`setEffekte_id`) REFERENCES `schiffstyp_modifikation` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jumpnodes`
--

DROP TABLE IF EXISTS `jumpnodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jumpnodes` (
                             `id` int NOT NULL AUTO_INCREMENT,
                             `gcpcolonistblock` tinyint(1) NOT NULL,
                             `hidden` int NOT NULL,
                             `name` varchar(255) NOT NULL,
                             `star_system` int DEFAULT NULL,
                             `systemout` int NOT NULL,
                             `wpnblock` tinyint(1) NOT NULL,
                             `x` int NOT NULL,
                             `xout` int NOT NULL,
                             `y` int NOT NULL,
                             `yout` int NOT NULL,
                             PRIMARY KEY (`id`),
                             KEY `jumpnode_coords` (`x`,`y`,`star_system`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jumps`
--

DROP TABLE IF EXISTS `jumps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jumps` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `star_system` int DEFAULT NULL,
                         `version` int NOT NULL,
                         `x` int NOT NULL,
                         `y` int NOT NULL,
                         `shipid` int NOT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `shipid` (`shipid`),
                         KEY `jumps_fk_ships` (`shipid`),
                         CONSTRAINT `jumps_fk_ships` FOREIGN KEY (`shipid`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kaserne`
--

DROP TABLE IF EXISTS `kaserne`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kaserne` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `col` int NOT NULL,
                           PRIMARY KEY (`id`),
                           KEY `kaserne_fk_bases` (`col`),
                           CONSTRAINT `kaserne_fk_bases` FOREIGN KEY (`col`) REFERENCES `bases` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1120 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kaserne_queues`
--

DROP TABLE IF EXISTS `kaserne_queues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kaserne_queues` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `count` int NOT NULL,
                                  `remaining` int NOT NULL,
                                  `kaserne` int NOT NULL,
                                  `unitid` int NOT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `kaserne_queues_fk_unittype` (`unitid`),
                                  KEY `kaserne_queues_fk_kaserne` (`kaserne`),
                                  CONSTRAINT `kaserne_queues_fk_kaserne` FOREIGN KEY (`kaserne`) REFERENCES `kaserne` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                  CONSTRAINT `kaserne_queues_fk_unittype` FOREIGN KEY (`unitid`) REFERENCES `unit_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=377 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `logging`
--

DROP TABLE IF EXISTS `logging`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `logging` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `data` longtext,
                           `source` varchar(255) NOT NULL,
                           `target` varchar(255) NOT NULL,
                           `time` bigint NOT NULL,
                           `type` varchar(255) NOT NULL,
                           `user_id` int NOT NULL,
                           `version` int NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=611825 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loyalitaetspunkte`
--

DROP TABLE IF EXISTS `loyalitaetspunkte`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loyalitaetspunkte` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `anmerkungen` longtext,
                                     `anzahlPunkte` int NOT NULL,
                                     `grund` varchar(255) NOT NULL,
                                     `zeitpunkt` datetime NOT NULL,
                                     `user_id` int NOT NULL,
                                     `verliehenDurch_id` int NOT NULL,
                                     PRIMARY KEY (`id`),
                                     KEY `loyalitaetspunkte_fk_users_1` (`user_id`),
                                     KEY `loyalitaetspunkte_fk_users_2` (`verliehenDurch_id`),
                                     CONSTRAINT `loyalitaetspunkte_fk_users_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT `loyalitaetspunkte_fk_users_2` FOREIGN KEY (`verliehenDurch_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2253 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medal`
--

DROP TABLE IF EXISTS `medal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medal` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `adminOnly` tinyint(1) NOT NULL,
                         `image` varchar(255) DEFAULT NULL,
                         `imageSmall` varchar(255) DEFAULT NULL,
                         `name` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `module_slot`
--

DROP TABLE IF EXISTS `module_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `module_slot` (
                               `slottype` varchar(255) NOT NULL,
                               `name` varchar(255) DEFAULT NULL,
                               `parent_slottype` varchar(255) DEFAULT NULL,
                               PRIMARY KEY (`slottype`),
                               KEY `module_slot_fk_module_slot` (`parent_slottype`),
                               CONSTRAINT `module_slot_fk_module_slot` FOREIGN KEY (`parent_slottype`) REFERENCES `module_slot` (`slottype`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `nebel`
--

DROP TABLE IF EXISTS `nebel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nebel` (
                         `star_system` int NOT NULL,
                         `x` int NOT NULL,
                         `y` int NOT NULL,
                         `type` int NOT NULL,
                         PRIMARY KEY (`star_system`,`x`,`y`),
                         KEY `idx_nebulatype` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `offiziere`
--

DROP TABLE IF EXISTS `offiziere`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `offiziere` (
                             `id` int NOT NULL AUTO_INCREMENT,
                             `com` int NOT NULL,
                             `comu` int NOT NULL,
                             `ing` int NOT NULL,
                             `ingu` int NOT NULL,
                             `name` varchar(255) NOT NULL,
                             `nav` int NOT NULL,
                             `navu` int NOT NULL,
                             `rang` int NOT NULL,
                             `sec` int NOT NULL,
                             `secu` int NOT NULL,
                             `spec` int NOT NULL,
                             `training` tinyint(1) NOT NULL,
                             `waf` int NOT NULL,
                             `wafu` int NOT NULL,
                             `userid` int NOT NULL,
                             `stationiertAufBasis_id` int DEFAULT NULL,
                             `stationiertAufSchiff_id` int DEFAULT NULL,
                             PRIMARY KEY (`id`),
                             KEY `offiziere_fk_ships` (`stationiertAufSchiff_id`),
                             KEY `offiziere_fk_bases` (`stationiertAufBasis_id`),
                             KEY `offiziere_fk_users` (`userid`),
                             CONSTRAINT `offiziere_fk_bases` FOREIGN KEY (`stationiertAufBasis_id`) REFERENCES `bases` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                             CONSTRAINT `offiziere_fk_ships` FOREIGN KEY (`stationiertAufSchiff_id`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                             CONSTRAINT `offiziere_fk_users` FOREIGN KEY (`userid`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=579219 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
                          `ordertype` varchar(31) NOT NULL,
                          `id` int NOT NULL AUTO_INCREMENT,
                          `tick` int NOT NULL,
                          `user_id` int DEFAULT NULL,
                          `version` int NOT NULL,
                          `type` int DEFAULT NULL,
                          `flags` varchar(255) DEFAULT NULL,
                          `shipType_id` int DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          KEY `orders_fk_user` (`user_id`),
                          KEY `order_ship_fk_ship_type` (`shipType_id`),
                          CONSTRAINT `order_ship_fk_ship_type` FOREIGN KEY (`shipType_id`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                          CONSTRAINT `orders_fk_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders_offiziere`
--

DROP TABLE IF EXISTS `orders_offiziere`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders_offiziere` (
                                    `id` int NOT NULL AUTO_INCREMENT,
                                    `com` int NOT NULL,
                                    `cost` int NOT NULL,
                                    `ing` int NOT NULL,
                                    `name` varchar(255) NOT NULL,
                                    `nav` int NOT NULL,
                                    `rang` int NOT NULL,
                                    `sec` int NOT NULL,
                                    `waf` int NOT NULL,
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders_ships`
--

DROP TABLE IF EXISTS `orders_ships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders_ships` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `cost` int NOT NULL,
                                `rasse_id` int DEFAULT NULL,
                                `type` int NOT NULL,
                                PRIMARY KEY (`id`),
                                KEY `orders_ships_fk_shiptypes` (`type`),
                                KEY `orderable_ship_fk_rasse` (`rasse_id`),
                                CONSTRAINT `orderable_ship_fk_rasse` FOREIGN KEY (`rasse_id`) REFERENCES `rasse` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `orders_ships_fk_shiptypes` FOREIGN KEY (`type`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=131 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ordner`
--

DROP TABLE IF EXISTS `ordner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ordner` (
                          `id` int NOT NULL AUTO_INCREMENT,
                          `flags` int NOT NULL,
                          `name` varchar(255) NOT NULL,
                          `parent` int NOT NULL,
                          `version` int NOT NULL,
                          `playerid` int NOT NULL,
                          PRIMARY KEY (`id`),
                          KEY `ordner_fk_users` (`playerid`),
                          CONSTRAINT `ordner_fk_users` FOREIGN KEY (`playerid`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=3870 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `action` varchar(255) COLLATE utf8_bin NOT NULL,
                              `category` varchar(255) COLLATE utf8_bin NOT NULL,
                              `user_id` int NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `permission_fk_users` (`user_id`),
                              CONSTRAINT `permission_fk_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=575 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_news`
--

DROP TABLE IF EXISTS `portal_news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_news` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `author` longtext NOT NULL,
                               `date` bigint NOT NULL,
                               `txt` longtext NOT NULL,
                               `shortDescription` longtext NOT NULL,
                               `title` varchar(255) NOT NULL,
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rang`
--

DROP TABLE IF EXISTS `rang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rang` (
                        `id` int NOT NULL,
                        `image` varchar(255) DEFAULT NULL,
                        `name` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rasse`
--

DROP TABLE IF EXISTS `rasse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rasse` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `description` longtext,
                         `name` varchar(255) DEFAULT NULL,
                         `personenNamenGenerator` varchar(255) DEFAULT NULL,
                         `playable` tinyint(1) NOT NULL,
                         `playableext` tinyint(1) NOT NULL,
                         `schiffsKlassenNamenGenerator` varchar(255) DEFAULT NULL,
                         `schiffsNamenGenerator` varchar(255) DEFAULT NULL,
                         `head_id` int DEFAULT NULL,
                         `memberIn_id` int DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `rasse_fk_rasse` (`memberIn_id`),
                         KEY `rasse_fk_user` (`head_id`),
                         CONSTRAINT `rasse_fk_rasse` FOREIGN KEY (`memberIn_id`) REFERENCES `rasse` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `rasse_fk_user` FOREIGN KEY (`head_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schiff_einstellungen`
--

DROP TABLE IF EXISTS `schiff_einstellungen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schiff_einstellungen` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `autodeut` tinyint NOT NULL,
                                        `automine` tinyint NOT NULL,
                                        `bookmark` tinyint(1) NOT NULL,
                                        `destcom` longtext NOT NULL,
                                        `destsystem` int NOT NULL,
                                        `destx` int NOT NULL,
                                        `desty` int NOT NULL,
                                        `gotoSecondRow` tinyint(1) NOT NULL DEFAULT '1',
                                        `useInstantBattleEnter` tinyint(1) NOT NULL DEFAULT '1',
                                        `isallyfeeding` tinyint(1) NOT NULL,
                                        `isfeeding` tinyint(1) NOT NULL,
                                        `showtradepost` int NOT NULL,
                                        `startFighters` tinyint(1) NOT NULL,
                                        `version` int NOT NULL,
                                        PRIMARY KEY (`id`),
                                        KEY `schiffeinstellungen_feeding` (`isfeeding`),
                                        KEY `schiffeinstellungen_bookmark` (`bookmark`),
                                        KEY `schiffeinstellungen_allyfeeding` (`isallyfeeding`)
) ENGINE=InnoDB AUTO_INCREMENT=1349080 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schiffsbauplan_forschungen`
--

DROP TABLE IF EXISTS `schiffsbauplan_forschungen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schiffsbauplan_forschungen` (
                                              `items_id` int NOT NULL,
                                              `benoetigteForschungen_id` int NOT NULL,
                                              PRIMARY KEY (`items_id`,`benoetigteForschungen_id`),
                                              KEY `schiffsbauplan_forschungen_fk_schiffsbauplan` (`benoetigteForschungen_id`),
                                              KEY `schiffsbauplan_fk_forschungen` (`items_id`),
                                              CONSTRAINT `schiffsbauplan_fk_forschungen` FOREIGN KEY (`items_id`) REFERENCES `items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                              CONSTRAINT `schiffsbauplan_forschungen_fk_schiffsbauplan` FOREIGN KEY (`benoetigteForschungen_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schiffsmodul_slots`
--

DROP TABLE IF EXISTS `schiffsmodul_slots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schiffsmodul_slots` (
                                      `Schiffsmodul_id` int NOT NULL,
                                      `slots` varchar(255) DEFAULT NULL,
                                      KEY `schiffsmodul_slots_fk_schiffsmodul` (`Schiffsmodul_id`),
                                      CONSTRAINT `schiffsmodul_slots_fk_schiffsmodul` FOREIGN KEY (`Schiffsmodul_id`) REFERENCES `items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schiffstyp_modifikation`
--

DROP TABLE IF EXISTS `schiffstyp_modifikation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schiffstyp_modifikation` (
                                           `id` bigint NOT NULL AUTO_INCREMENT,
                                           `aDocks` int NOT NULL,
                                           `ablativeArmor` int NOT NULL,
                                           `bounty` decimal(19,2) DEFAULT NULL,
                                           `cargo` bigint NOT NULL,
                                           `cost` int NOT NULL,
                                           `crew` int NOT NULL,
                                           `deutFactor` int NOT NULL,
                                           `eps` int NOT NULL,
                                           `heat` int NOT NULL,
                                           `hull` int NOT NULL,
                                           `hydro` int NOT NULL,
                                           `jDocks` int NOT NULL,
                                           `lostInEmpChance` double NOT NULL,
                                           `maxunitsize` int NOT NULL,
                                           `minCrew` int NOT NULL,
                                           `nahrungcargo` bigint NOT NULL,
                                           `nickname` varchar(255) DEFAULT NULL,
                                           `oneWayWerft_id` int DEFAULT NULL,
                                           `panzerung` int NOT NULL,
                                           `picture` varchar(255) DEFAULT NULL,
                                           `ra` int NOT NULL,
                                           `rd` int NOT NULL,
                                           `reCost` int NOT NULL,
                                           `rm` int NOT NULL,
                                           `ru` int NOT NULL,
                                           `sensorRange` int NOT NULL,
                                           `shields` int NOT NULL,
                                           `size` int NOT NULL,
                                           `srs` tinyint(1) DEFAULT NULL,
                                           `torpedoDef` int NOT NULL,
                                           `unitspace` int NOT NULL,
                                           `version` int NOT NULL,
                                           `werft` int NOT NULL,
                                           `produces` varchar(255) NOT NULL DEFAULT '',
                                           PRIMARY KEY (`id`),
                                           KEY `schiffstypmodifikation_fk_schiffstyp` (`oneWayWerft_id`),
                                           CONSTRAINT `schiffstypmodifikation_fk_schiffstyp` FOREIGN KEY (`oneWayWerft_id`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=9583 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schiffstyp_modifikation_flags`
--

DROP TABLE IF EXISTS `schiffstyp_modifikation_flags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schiffstyp_modifikation_flags` (
                                                 `SchiffstypModifikation_id` bigint NOT NULL,
                                                 `flags` int DEFAULT NULL,
                                                 KEY `schiffstypmodifikation_flags_fk_schiffstypmodifikation` (`SchiffstypModifikation_id`),
                                                 CONSTRAINT `schiffstypmodifikation_flags_fk_schiffstypmodifikation` FOREIGN KEY (`SchiffstypModifikation_id`) REFERENCES `schiffstyp_modifikation` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schiffswaffenkonfiguration`
--

DROP TABLE IF EXISTS `schiffswaffenkonfiguration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schiffswaffenkonfiguration` (
                                              `id` bigint NOT NULL AUTO_INCREMENT,
                                              `anzahl` int NOT NULL,
                                              `hitze` int NOT NULL,
                                              `maxUeberhitzung` int NOT NULL,
                                              `version` int NOT NULL,
                                              `waffe_id` varchar(255) DEFAULT NULL,
                                              `schiffstyp_modifikation_id` bigint DEFAULT NULL,
                                              PRIMARY KEY (`id`),
                                              KEY `weapon_changeset_fk_weapon` (`waffe_id`),
                                              KEY `schiffstyp_modifikation_waffen_fk_schiffstyp_modifikation` (`schiffstyp_modifikation_id`),
                                              CONSTRAINT `schiffstyp_modifikation_waffen_fk_schiffstyp_modifikation` FOREIGN KEY (`schiffstyp_modifikation_id`) REFERENCES `schiffstyp_modifikation` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                              CONSTRAINT `weapon_changeset_fk_weapon` FOREIGN KEY (`waffe_id`) REFERENCES `weapon` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schlacht_log`
--

DROP TABLE IF EXISTS `schlacht_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schlacht_log` (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `startTick` int NOT NULL,
                                `startZeitpunkt` datetime DEFAULT NULL,
                                `star_system` int DEFAULT NULL,
                                `version` int DEFAULT NULL,
                                `x` int NOT NULL,
                                `y` int NOT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8505 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schlacht_log_eintrag`
--

DROP TABLE IF EXISTS `schlacht_log_eintrag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schlacht_log_eintrag` (
                                        `DTYPE` varchar(31) NOT NULL,
                                        `id` bigint NOT NULL AUTO_INCREMENT,
                                        `tick` int NOT NULL,
                                        `version` int DEFAULT NULL,
                                        `zeitpunkt` datetime DEFAULT NULL,
                                        `seite` int DEFAULT NULL,
                                        `text` longtext,
                                        `allianzId` int DEFAULT NULL,
                                        `name` varchar(255) DEFAULT NULL,
                                        `userId` int DEFAULT NULL,
                                        `typ` int DEFAULT NULL,
                                        `schlachtlog_id` bigint DEFAULT NULL,
                                        PRIMARY KEY (`id`),
                                        KEY `schlachtlogeintrag_fk_schlachtlog` (`schlachtlog_id`),
                                        CONSTRAINT `schlachtlogeintrag_fk_schlachtlog` FOREIGN KEY (`schlachtlog_id`) REFERENCES `schlacht_log` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1861482 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sessions` (
                            `id` int NOT NULL AUTO_INCREMENT,
                            `tick` bigint NOT NULL,
                            `token` varchar(255) NOT NULL,
                            `version` int NOT NULL,
                            `userId` int NOT NULL,
                            PRIMARY KEY (`id`),
                            KEY `sessions_fk_users` (`userId`),
                            CONSTRAINT `sessions_fk_users` FOREIGN KEY (`userId`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ship_flags`
--

DROP TABLE IF EXISTS `ship_flags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ship_flags` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `flagType` int NOT NULL,
                              `remaining` int NOT NULL,
                              `version` int NOT NULL,
                              `ship` int NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `ship_flags_fk_ships` (`ship`),
                              CONSTRAINT `ship_flags_fk_ships` FOREIGN KEY (`ship`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=507282 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ship_fleets`
--

DROP TABLE IF EXISTS `ship_fleets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ship_fleets` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `name` varchar(255) NOT NULL,
                               `version` int NOT NULL,
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=212026 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ship_history`
--

DROP TABLE IF EXISTS `ship_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ship_history` (
                                `id` int NOT NULL,
                                `history` longtext COLLATE utf8_bin,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ship_loot`
--

DROP TABLE IF EXISTS `ship_loot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ship_loot` (
                             `id` int NOT NULL AUTO_INCREMENT,
                             `chance` int NOT NULL,
                             `count` int NOT NULL,
                             `resource` varchar(255) NOT NULL,
                             `shiptype` int NOT NULL,
                             `totalmax` int NOT NULL,
                             `version` int NOT NULL,
                             `owner` int NOT NULL,
                             `targetuser` int NOT NULL,
                             PRIMARY KEY (`id`),
                             KEY `ship_loot_fk_users2` (`targetuser`),
                             KEY `ship_loot_fk_users1` (`owner`),
                             KEY `shiploot_shiptype` (`shiptype`),
                             CONSTRAINT `ship_loot_fk_users1` FOREIGN KEY (`owner`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                             CONSTRAINT `ship_loot_fk_users2` FOREIGN KEY (`targetuser`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ship_script_data`
--

DROP TABLE IF EXISTS `ship_script_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ship_script_data` (
                                    `shipid` int NOT NULL AUTO_INCREMENT,
                                    `script` longtext,
                                    `scriptexedata` longblob,
                                    PRIMARY KEY (`shipid`)
) ENGINE=InnoDB AUTO_INCREMENT=1015742 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ship_types`
--

DROP TABLE IF EXISTS `ship_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ship_types` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `adocks` int NOT NULL,
                              `ablativeArmor` int NOT NULL,
                              `bounty` decimal(19,2) NOT NULL,
                              `cargo` bigint NOT NULL,
                              `chance4Loot` int NOT NULL,
                              `cost` int NOT NULL,
                              `crew` int NOT NULL,
                              `descrip` longtext NOT NULL,
                              `deutfactor` int NOT NULL,
                              `eps` int NOT NULL,
                              `flags` longtext NOT NULL,
                              `groupwrap` int NOT NULL,
                              `heat` int NOT NULL,
                              `hide` tinyint(1) NOT NULL,
                              `hull` int NOT NULL,
                              `hydro` int NOT NULL,
                              `jdocks` int NOT NULL,
                              `lostInEmpChance` double NOT NULL,
                              `maxheat` longtext NOT NULL,
                              `maxunitsize` int NOT NULL,
                              `minCrew` int NOT NULL,
                              `modules` longtext NOT NULL,
                              `nahrungcargo` bigint NOT NULL,
                              `nickname` varchar(255) NOT NULL,
                              `panzerung` int NOT NULL,
                              `picture` varchar(255) NOT NULL,
                              `ra` int NOT NULL,
                              `rd` int NOT NULL,
                              `recost` int NOT NULL,
                              `rm` int NOT NULL,
                              `ru` int NOT NULL,
                              `sensorrange` int NOT NULL,
                              `shields` int NOT NULL,
                              `class` int NOT NULL,
                              `size` int NOT NULL,
                              `srs` tinyint(1) NOT NULL,
                              `torpedodef` int NOT NULL,
                              `unitspace` int NOT NULL,
                              `version` int NOT NULL,
                              `versorger` tinyint(1) NOT NULL,
                              `weapons` longtext NOT NULL,
                              `werft` int NOT NULL,
                              `ow_werft` int DEFAULT NULL,
                              `produces` varchar(255) NOT NULL DEFAULT '',
                              PRIMARY KEY (`id`),
                              KEY `ship_types_fk_ship_types` (`ow_werft`),
                              KEY `shiptype_versorger` (`versorger`),
                              CONSTRAINT `ship_types_fk_ship_types` FOREIGN KEY (`ow_werft`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=281 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ships`
--

DROP TABLE IF EXISTS `ships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ships` (
                         `id` int NOT NULL,
                         `ablativeArmor` int NOT NULL,
                         `alarm` int NOT NULL,
                         `battleAction` tinyint(1) NOT NULL,
                         `cargo` longtext NOT NULL,
                         `comm` int NOT NULL,
                         `crew` int NOT NULL,
                         `docked` varchar(255) NOT NULL,
                         `e` int NOT NULL,
                         `engine` int NOT NULL,
                         `s` int NOT NULL,
                         `hull` int NOT NULL,
                         `jumptarget` varchar(255) NOT NULL,
                         `nahrungcargo` bigint NOT NULL,
                         `name` varchar(255) NOT NULL,
                         `sensors` int NOT NULL,
                         `shields` int NOT NULL,
                         `status` varchar(255) NOT NULL,
                         `star_system` int DEFAULT NULL,
                         `version` int NOT NULL,
                         `heat` longtext NOT NULL,
                         `weapons` int NOT NULL,
                         `x` int NOT NULL,
                         `y` int NOT NULL,
                         `battle` int DEFAULT NULL,
                         `einstellungen_id` int DEFAULT NULL,
                         `fleet` int DEFAULT NULL,
                         `modules` int DEFAULT NULL,
                         `owner` int NOT NULL,
                         `scriptData_id` int DEFAULT NULL,
                         `type` int NOT NULL,
                         PRIMARY KEY (`id`),
                         KEY `ships_fk_ship_fleets` (`fleet`),
                         KEY `ships_fk_ship_script_data` (`scriptData_id`),
                         KEY `ships_fk_schiff_einstellungen` (`einstellungen_id`),
                         KEY `ships_fk_battles` (`battle`),
                         KEY `ships_type_fk` (`type`),
                         KEY `ships_fk_users` (`owner`),
                         KEY `ships_fk_ships_modules` (`modules`),
                         KEY `FK6856DB7B6B202B4` (`id`),
                         KEY `ship_coords` (`star_system`,`x`,`y`),
                         KEY `ship_owner` (`owner`,`id`),
                         KEY `ship_docked` (`docked`),
                         KEY `ship_status` (`status`),
                         CONSTRAINT `FK6856DB7B6B202B4` FOREIGN KEY (`id`) REFERENCES `ship_history` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_fk_battles` FOREIGN KEY (`battle`) REFERENCES `battles` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_fk_schiff_einstellungen` FOREIGN KEY (`einstellungen_id`) REFERENCES `schiff_einstellungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_fk_ship_fleets` FOREIGN KEY (`fleet`) REFERENCES `ship_fleets` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_fk_ship_script_data` FOREIGN KEY (`scriptData_id`) REFERENCES `ship_script_data` (`shipid`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_fk_ships_modules` FOREIGN KEY (`modules`) REFERENCES `ships_modules` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_fk_users` FOREIGN KEY (`owner`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `ships_type_fk` FOREIGN KEY (`type`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ships_baubar`
--

DROP TABLE IF EXISTS `ships_baubar`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ships_baubar` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `costs` longtext NOT NULL,
                                `crew` int NOT NULL,
                                `dauer` int NOT NULL,
                                `ekosten` int NOT NULL,
                                `flagschiff` tinyint(1) NOT NULL,
                                `race` int NOT NULL,
                                `res1_id` int DEFAULT NULL,
                                `res2_id` int DEFAULT NULL,
                                `res3_id` int DEFAULT NULL,
                                `werftslots` int NOT NULL,
                                `type` int NOT NULL,
                                PRIMARY KEY (`id`),
                                KEY `ships_baubar_type_fk` (`type`),
                                KEY `ships_baubar_fk_forschung2` (`res2_id`),
                                KEY `ships_baubar_fk_forschung1` (`res1_id`),
                                KEY `ships_baubar_fk_forschung3` (`res3_id`),
                                CONSTRAINT `ships_baubar_fk_forschung1` FOREIGN KEY (`res1_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `ships_baubar_fk_forschung2` FOREIGN KEY (`res2_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `ships_baubar_fk_forschung3` FOREIGN KEY (`res3_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `ships_baubar_type_fk` FOREIGN KEY (`type`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ships_lost`
--

DROP TABLE IF EXISTS `ships_lost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ships_lost` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `ally` int NOT NULL,
                              `battle` int NOT NULL,
                              `battlelog` varchar(255) DEFAULT NULL,
                              `destally` int NOT NULL,
                              `destowner` int NOT NULL,
                              `name` varchar(255) DEFAULT NULL,
                              `owner` int NOT NULL,
                              `tick` int NOT NULL,
                              `type` int NOT NULL,
                              `version` int NOT NULL,
                              `schlachtLog_id` bigint DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `shiplost_ally` (`ally`),
                              KEY `shiplost_battle` (`battle`),
                              KEY `shiplost_battlelog` (`battlelog`),
                              KEY `shiplost_destally` (`destally`),
                              KEY `shiplost_destowner` (`destowner`),
                              KEY `shiplost_owner` (`owner`),
                              KEY `shiplost_fk_schlachtlog` (`schlachtLog_id`),
                              CONSTRAINT `shiplost_fk_schlachtlog` FOREIGN KEY (`schlachtLog_id`) REFERENCES `schlacht_log` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1037538 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ships_modules`
--

DROP TABLE IF EXISTS `ships_modules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ships_modules` (
                                 `id` int NOT NULL AUTO_INCREMENT,
                                 `adocks` int NOT NULL,
                                 `ablativeArmor` int NOT NULL,
                                 `bounty` decimal(19,2) NOT NULL,
                                 `cargo` bigint NOT NULL,
                                 `cost` int NOT NULL,
                                 `crew` int NOT NULL,
                                 `deutfactor` int NOT NULL,
                                 `eps` int NOT NULL,
                                 `flags` longtext NOT NULL,
                                 `heat` int NOT NULL,
                                 `hull` int NOT NULL,
                                 `hydro` int NOT NULL,
                                 `jdocks` int NOT NULL,
                                 `lostInEmpChance` double NOT NULL,
                                 `maxheat` longtext NOT NULL,
                                 `maxunitsize` int NOT NULL,
                                 `minCrew` int NOT NULL,
                                 `modules` longtext NOT NULL,
                                 `nahrungcargo` bigint NOT NULL,
                                 `nickname` varchar(255) NOT NULL,
                                 `panzerung` int NOT NULL,
                                 `picture` varchar(255) NOT NULL,
                                 `ra` int NOT NULL,
                                 `rd` int NOT NULL,
                                 `recost` int NOT NULL,
                                 `rm` int NOT NULL,
                                 `ru` int NOT NULL,
                                 `sensorrange` int NOT NULL,
                                 `shields` int NOT NULL,
                                 `size` int NOT NULL,
                                 `srs` tinyint(1) NOT NULL,
                                 `torpedodef` int NOT NULL,
                                 `unitspace` int NOT NULL,
                                 `version` int NOT NULL,
                                 `versorger` tinyint(1) NOT NULL,
                                 `weapons` longtext NOT NULL,
                                 `werft` int NOT NULL,
                                 `ow_werft` int DEFAULT NULL,
                                 `produces` varchar(255) NOT NULL DEFAULT '',
                                 PRIMARY KEY (`id`),
                                 KEY `ship_modules_fk_ships_types` (`ow_werft`),
                                 KEY `shipmodules_versorger` (`versorger`),
                                 CONSTRAINT `ship_modules_fk_ships_types` FOREIGN KEY (`ow_werft`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1919982 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `skn`
--

DROP TABLE IF EXISTS `skn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skn` (
                       `post` int NOT NULL AUTO_INCREMENT,
                       `allypic` int NOT NULL,
                       `head` varchar(255) NOT NULL,
                       `name` varchar(255) NOT NULL,
                       `pic` int NOT NULL,
                       `text` longtext NOT NULL,
                       `tick` int NOT NULL,
                       `time` bigint NOT NULL,
                       `version` int NOT NULL,
                       `channel` int NOT NULL,
                       `userid` int NOT NULL,
                       PRIMARY KEY (`post`),
                       KEY `skn_fk_users` (`userid`),
                       KEY `skn_fk_skn_channels` (`channel`),
                       CONSTRAINT `skn_fk_skn_channels` FOREIGN KEY (`channel`) REFERENCES `skn_channels` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                       CONSTRAINT `skn_fk_users` FOREIGN KEY (`userid`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=47382 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `skn_channels`
--

DROP TABLE IF EXISTS `skn_channels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skn_channels` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `allyOwner_id` int DEFAULT NULL,
                                `name` varchar(255) NOT NULL,
                                `readall` tinyint(1) NOT NULL,
                                `readAlly_id` int DEFAULT NULL,
                                `readnpc` tinyint(1) NOT NULL,
                                `readplayer` longtext NOT NULL,
                                `version` int NOT NULL,
                                `writeall` tinyint(1) NOT NULL,
                                `writeAlly_id` int DEFAULT NULL,
                                `writenpc` tinyint(1) NOT NULL,
                                `writeplayer` longtext NOT NULL,
                                PRIMARY KEY (`id`),
                                KEY `comnet_channel_fk_ally` (`allyOwner_id`),
                                KEY `comnet_channel_fk_ally3` (`readAlly_id`),
                                KEY `comnet_channel_fk_ally2` (`writeAlly_id`),
                                CONSTRAINT `comnet_channel_fk_ally` FOREIGN KEY (`allyOwner_id`) REFERENCES `ally` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `comnet_channel_fk_ally2` FOREIGN KEY (`writeAlly_id`) REFERENCES `ally` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `comnet_channel_fk_ally3` FOREIGN KEY (`readAlly_id`) REFERENCES `ally` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=752 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `skn_visits`
--

DROP TABLE IF EXISTS `skn_visits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skn_visits` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `time` bigint NOT NULL,
                              `version` int NOT NULL,
                              `channel_id` int NOT NULL,
                              `user_id` int NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `skn_visits_user` (`user_id`,`channel_id`),
                              KEY `skn_visits_fk_skn_channels` (`channel_id`),
                              KEY `skn_visits_fk_users` (`user_id`),
                              CONSTRAINT `skn_visits_fk_skn_channels` FOREIGN KEY (`channel_id`) REFERENCES `skn_channels` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                              CONSTRAINT `skn_visits_fk_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=71900 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smilies`
--

DROP TABLE IF EXISTS `smilies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `smilies` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `image` varchar(255) NOT NULL,
                           `tag` varchar(255) NOT NULL,
                           `version` int NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stat_aktive_spieler`
--

DROP TABLE IF EXISTS `stat_aktive_spieler`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_aktive_spieler` (
                                       `tick` int NOT NULL,
                                       `aktiv` int NOT NULL,
                                       `gesamtanzahl` int NOT NULL,
                                       `inaktiv` int NOT NULL,
                                       `maxUserId` int NOT NULL,
                                       `registrierungen` int NOT NULL,
                                       `sehrAktiv` int NOT NULL,
                                       `teilweiseAktiv` int NOT NULL,
                                       `vacation` int NOT NULL,
                                       `version` int NOT NULL,
                                       `wenigAktiv` int NOT NULL,
                                       PRIMARY KEY (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats_cargo`
--

DROP TABLE IF EXISTS `stats_cargo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stats_cargo` (
                               `tick` int NOT NULL,
                               `cargo` longtext NOT NULL,
                               `version` int NOT NULL,
                               PRIMARY KEY (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats_gtu`
--

DROP TABLE IF EXISTS `stats_gtu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stats_gtu` (
                             `id` int NOT NULL AUTO_INCREMENT,
                             `gtugew` double NOT NULL,
                             `mtype` int NOT NULL,
                             `owner` int NOT NULL,
                             `ownername` varchar(255) NOT NULL,
                             `preis` bigint NOT NULL,
                             `type` longtext NOT NULL,
                             `userid` int NOT NULL,
                             `username` varchar(255) NOT NULL,
                             `version` int NOT NULL,
                             PRIMARY KEY (`id`),
                             KEY `preis` (`preis`)
) ENGINE=InnoDB AUTO_INCREMENT=26462 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats_module_locations`
--

DROP TABLE IF EXISTS `stats_module_locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stats_module_locations` (
                                          `id` int NOT NULL AUTO_INCREMENT,
                                          `item_id` int NOT NULL,
                                          `locations` varchar(255) NOT NULL,
                                          `version` int NOT NULL,
                                          `user_id` int NOT NULL,
                                          PRIMARY KEY (`id`),
                                          KEY `stats_module_locations_fk_user_id` (`user_id`),
                                          CONSTRAINT `stats_module_locations_fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=19262562 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats_ships`
--

DROP TABLE IF EXISTS `stats_ships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stats_ships` (
                               `tick` int NOT NULL,
                               `crewcount` bigint NOT NULL,
                               `shipcount` bigint NOT NULL,
                               `version` int NOT NULL,
                               PRIMARY KEY (`tick`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats_user_cargo`
--

DROP TABLE IF EXISTS `stats_user_cargo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stats_user_cargo` (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `cargo` longtext NOT NULL,
                                    `version` int NOT NULL,
                                    `user_id` int NOT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `user_id` (`user_id`),
                                    KEY `stats_user_cargo_fk_user_id` (`user_id`),
                                    CONSTRAINT `stats_user_cargo_fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=363982 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stats_verkaeufe`
--

DROP TABLE IF EXISTS `stats_verkaeufe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stats_verkaeufe` (
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `place` varchar(255) NOT NULL,
                                   `stats` longtext NOT NULL,
                                   `star_system` int DEFAULT NULL,
                                   `tick` int NOT NULL,
                                   `version` int NOT NULL,
                                   PRIMARY KEY (`id`),
                                   KEY `place` (`place`,`star_system`),
                                   KEY `tick` (`tick`),
                                   KEY `system` (`star_system`)
) ENGINE=InnoDB AUTO_INCREMENT=266254 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systems`
--

DROP TABLE IF EXISTS `systems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `systems` (
                           `id` int NOT NULL AUTO_INCREMENT,
                           `military` tinyint(1) NOT NULL,
                           `descrip` longtext,
                           `gtuDropZone` varchar(255) DEFAULT NULL,
                           `height` int NOT NULL,
                           `starmap` tinyint(1) NOT NULL,
                           `mapX` int NOT NULL,
                           `mapY` int NOT NULL,
                           `maxColonies` int NOT NULL,
                           `Name` varchar(255) NOT NULL,
                           `orderloc` longtext,
                           `spawnableress` longtext,
                           `access` int NOT NULL,
                           `width` int NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=621 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
                         `taskid` varchar(255) NOT NULL,
                         `data1` varchar(255) DEFAULT NULL,
                         `data2` varchar(255) DEFAULT NULL,
                         `data3` varchar(255) DEFAULT NULL,
                         `time` bigint NOT NULL,
                         `timeout` int NOT NULL,
                         `type` int NOT NULL,
                         `version` int NOT NULL,
                         PRIMARY KEY (`taskid`),
                         KEY `taskkey_idx` (`type`,`time`,`data1`,`data2`,`data3`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tradepost_buy_limit`
--

DROP TABLE IF EXISTS `tradepost_buy_limit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tradepost_buy_limit` (
                                       `id` bigint NOT NULL AUTO_INCREMENT,
                                       `maximum` bigint NOT NULL,
                                       `min_rank` int NOT NULL,
                                       `resourceid` int NOT NULL,
                                       `version` int NOT NULL,
                                       `shipid` int NOT NULL,
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `shipid` (`shipid`,`resourceid`),
                                       KEY `tradepost_buy_limit_fk_ships` (`shipid`),
                                       CONSTRAINT `tradepost_buy_limit_fk_ships` FOREIGN KEY (`shipid`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=4481 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tradepost_sell`
--

DROP TABLE IF EXISTS `tradepost_sell`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tradepost_sell` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `minimum` bigint NOT NULL,
                                  `min_rank` int NOT NULL,
                                  `price` bigint NOT NULL,
                                  `resourceid` int NOT NULL,
                                  `version` int NOT NULL,
                                  `shipid` int NOT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `shipid` (`shipid`,`resourceid`),
                                  KEY `tradepost_sell_fk_ships` (`shipid`),
                                  CONSTRAINT `tradepost_sell_fk_ships` FOREIGN KEY (`shipid`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=6073 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transmissionen`
--

DROP TABLE IF EXISTS `transmissionen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transmissionen` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `flags` int NOT NULL,
                                  `gelesen` int NOT NULL,
                                  `inhalt` longtext NOT NULL,
                                  `kommentar` longtext NOT NULL,
                                  `ordner` int NOT NULL,
                                  `time` bigint NOT NULL,
                                  `title` varchar(255) NOT NULL,
                                  `version` int NOT NULL,
                                  `empfaenger` int NOT NULL,
                                  `sender` int NOT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `empfaenger` (`empfaenger`,`gelesen`),
                                  KEY `transmissionen_fk_users1` (`sender`),
                                  KEY `transmissionen_fk_users2` (`empfaenger`),
                                  CONSTRAINT `transmissionen_fk_users1` FOREIGN KEY (`sender`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                  CONSTRAINT `transmissionen_fk_users2` FOREIGN KEY (`empfaenger`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=8107234 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unit_types`
--

DROP TABLE IF EXISTS `unit_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `unit_types` (
                              `id` int NOT NULL AUTO_INCREMENT,
                              `buildcosts` varchar(255) NOT NULL,
                              `dauer` int NOT NULL,
                              `description` longtext,
                              `hidden` tinyint(1) NOT NULL,
                              `kapervalue` int NOT NULL,
                              `nahrungcost` double NOT NULL,
                              `name` varchar(255) NOT NULL,
                              `picture` varchar(255) NOT NULL,
                              `recost` double NOT NULL,
                              `res_id` int NOT NULL,
                              `size` int NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `unitttype_fk_forschung` (`res_id`),
                              CONSTRAINT `unitttype_fk_forschung` FOREIGN KEY (`res_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=303 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upgrade_info`
--

DROP TABLE IF EXISTS `upgrade_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upgrade_info` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `miningexplosive` int NOT NULL,
                                `modWert` int NOT NULL,
                                `ore` int NOT NULL,
                                `price` int NOT NULL,
                                `type_id` int NOT NULL,
                                `maxticks` int NOT NULL,
                                `minticks` int NOT NULL,
                                `upgradetype` int NOT NULL,
                                PRIMARY KEY (`id`),
                                KEY `upgrade_info_fk_basetype` (`type_id`),
                                CONSTRAINT `upgrade_info_fk_basetype` FOREIGN KEY (`type_id`) REFERENCES `base_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=95 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upgrade_job`
--

DROP TABLE IF EXISTS `upgrade_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upgrade_job` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `bar` tinyint(1) NOT NULL,
                               `endTick` int NOT NULL,
                               `payed` tinyint(1) NOT NULL,
                               `baseid` int NOT NULL,
                               `colonizerid` int DEFAULT NULL,
                               `userid` int NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `upgrade_job_fk_ships` (`colonizerid`),
                               KEY `upgrade_job_fk_base` (`baseid`),
                               KEY `upgrade_job_fk_user` (`userid`),
                               CONSTRAINT `upgrade_job_fk_base` FOREIGN KEY (`baseid`) REFERENCES `bases` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `upgrade_job_fk_ships` FOREIGN KEY (`colonizerid`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                               CONSTRAINT `upgrade_job_fk_user` FOREIGN KEY (`userid`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upgrade_job_upgrade_info`
--

DROP TABLE IF EXISTS `upgrade_job_upgrade_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upgrade_job_upgrade_info` (
                                            `upgrade_job_id` int NOT NULL,
                                            `upgradelist_id` int NOT NULL,
                                            PRIMARY KEY (`upgrade_job_id`,`upgradelist_id`),
                                            KEY `upgrade_info_fk_upgrade_job` (`upgradelist_id`),
                                            KEY `upgrade_job_fk_mod` (`upgrade_job_id`),
                                            CONSTRAINT `upgrade_info_fk_upgrade_job` FOREIGN KEY (`upgradelist_id`) REFERENCES `upgrade_info` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                            CONSTRAINT `upgrade_job_fk_mod` FOREIGN KEY (`upgrade_job_id`) REFERENCES `upgrade_job` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `upgrade_maxvalues`
--

DROP TABLE IF EXISTS `upgrade_maxvalues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `upgrade_maxvalues` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `maximalwert` int NOT NULL,
                                     `upgradetype` int NOT NULL,
                                     `type_id` int NOT NULL,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `type_id` (`type_id`,`upgradetype`),
                                     KEY `upgrade_max_values_fk_basetype` (`type_id`),
                                     CONSTRAINT `upgrade_max_values_fk_basetype` FOREIGN KEY (`type_id`) REFERENCES `base_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_moneytransfer`
--

DROP TABLE IF EXISTS `user_moneytransfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_moneytransfer` (
                                      `id` int NOT NULL AUTO_INCREMENT,
                                      `count` decimal(19,2) NOT NULL,
                                      `fake` int NOT NULL,
                                      `text` longtext NOT NULL,
                                      `time` bigint NOT NULL,
                                      `type` int NOT NULL,
                                      `version` int NOT NULL,
                                      `from_id` int NOT NULL,
                                      `to_id` int NOT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `time` (`time`),
                                      KEY `user_moneytransfer_fk_users2` (`to_id`),
                                      KEY `user_moneytransfer_fk_users1` (`from_id`),
                                      KEY `from_idx` (`from_id`,`to_id`),
                                      CONSTRAINT `user_moneytransfer_fk_users1` FOREIGN KEY (`from_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                      CONSTRAINT `user_moneytransfer_fk_users2` FOREIGN KEY (`to_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=611167 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_rank`
--

DROP TABLE IF EXISTS `user_rank`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_rank` (
                             `rank_id` int DEFAULT NULL,
                             `version` int NOT NULL,
                             `rank_giver` int NOT NULL,
                             `owner` int NOT NULL,
                             PRIMARY KEY (`owner`,`rank_giver`),
                             KEY `user_rank_fk_users1` (`owner`),
                             KEY `user_rank_fk_users2` (`rank_giver`),
                             CONSTRAINT `user_rank_fk_users1` FOREIGN KEY (`owner`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                             CONSTRAINT `user_rank_fk_users2` FOREIGN KEY (`rank_giver`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_relations`
--

DROP TABLE IF EXISTS `user_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_relations` (
                                  `id` int NOT NULL AUTO_INCREMENT,
                                  `status` int NOT NULL,
                                  `version` int NOT NULL,
                                  `target_id` int NOT NULL,
                                  `user_id` int NOT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `user_id` (`user_id`,`target_id`),
                                  KEY `user_relations_fk_users1` (`user_id`),
                                  KEY `user_relations_fk_users2` (`target_id`),
                                  CONSTRAINT `user_relations_fk_users1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                  CONSTRAINT `user_relations_fk_users2` FOREIGN KEY (`target_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=135368 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_values`
--

DROP TABLE IF EXISTS `user_values`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_values` (
                               `id` int NOT NULL AUTO_INCREMENT,
                               `name` varchar(255) NOT NULL,
                               `value` longtext NOT NULL,
                               `version` int NOT NULL,
                               `user_id` int NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `user_values_fk_users` (`user_id`),
                               KEY `uservalue_id` (`user_id`,`name`),
                               CONSTRAINT `user_values_fk_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=11370 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `id` int NOT NULL,
                         `accesslevel` int NOT NULL,
                         `disabled` tinyint NOT NULL,
                         `email` varchar(255) NOT NULL,
                         `flags` longtext NOT NULL,
                         `inakt` int NOT NULL,
                         `log_fail` int NOT NULL,
                         `name` varchar(255) NOT NULL,
                         `nickname` varchar(255) NOT NULL,
                         `passwort` varchar(255) NOT NULL,
                         `plainname` varchar(255) NOT NULL,
                         `signup` int NOT NULL,
                         `un` varchar(255) NOT NULL,
                         `version` int NOT NULL,
                         `bounty` decimal(19,2) NOT NULL,
                         `destroyedShips` int DEFAULT NULL,
                         `gtudropzone` int DEFAULT NULL,
                         `history` longtext,
                         `knownItems` longtext,
                         `konto` decimal(19,2) DEFAULT NULL,
                         `lostBattles` smallint DEFAULT NULL,
                         `lostShips` int DEFAULT NULL,
                         `medals` varchar(255) DEFAULT NULL,
                         `npcorderloc` varchar(255) DEFAULT NULL,
                         `npcpunkte` int DEFAULT NULL,
                         `personenNamenGenerator` varchar(255) DEFAULT NULL,
                         `race` int DEFAULT NULL,
                         `rang` int DEFAULT NULL,
                         `schiffsKlassenNamenGenerator` varchar(255) DEFAULT NULL,
                         `schiffsNamenGenerator` varchar(255) DEFAULT NULL,
                         `specializationPoints` int DEFAULT NULL,
                         `vaccount` int DEFAULT NULL,
                         `vacpoints` int DEFAULT NULL,
                         `wait4vac` int DEFAULT NULL,
                         `wonBattles` smallint DEFAULT NULL,
                         `ally` int DEFAULT NULL,
                         `allyposten` int DEFAULT NULL,
                         `apikey` varchar(25) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `allyposten` (`allyposten`),
                         KEY `vaccount` (`vaccount`,`wait4vac`),
                         KEY `users_fk_ally_posten` (`allyposten`),
                         KEY `users_fk_ally` (`ally`),
                         KEY `user_un` (`un`),
                         KEY `basicuser_un` (`un`),
                         CONSTRAINT `users_fk_ally` FOREIGN KEY (`ally`) REFERENCES `ally` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                         CONSTRAINT `users_fk_ally_posten` FOREIGN KEY (`allyposten`) REFERENCES `ally_posten` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users_forschungen`
--

DROP TABLE IF EXISTS `users_forschungen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_forschungen` (
                                     `users_id` int NOT NULL,
                                     `forschungen_id` int NOT NULL,
                                     PRIMARY KEY (`users_id`,`forschungen_id`),
                                     KEY `users_forschungen_fk_users` (`forschungen_id`),
                                     KEY `users_fk_forschungen` (`users_id`),
                                     CONSTRAINT `users_fk_forschungen` FOREIGN KEY (`users_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                     CONSTRAINT `users_forschungen_fk_users` FOREIGN KEY (`forschungen_id`) REFERENCES `forschungen` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `versteigerungen`
--

DROP TABLE IF EXISTS `versteigerungen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `versteigerungen` (
                                   `mtype` int NOT NULL,
                                   `id` int NOT NULL AUTO_INCREMENT,
                                   `preis` bigint NOT NULL,
                                   `tick` int NOT NULL,
                                   `version` int NOT NULL,
                                   `type` varchar(255) DEFAULT NULL,
                                   `bieter` int NOT NULL,
                                   `owner` int NOT NULL,
                                   PRIMARY KEY (`id`),
                                   KEY `versteigerungen_fk_users` (`bieter`),
                                   KEY `versteigerungen_fk_users2` (`owner`),
                                   CONSTRAINT `versteigerungen_fk_users` FOREIGN KEY (`bieter`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                   CONSTRAINT `versteigerungen_fk_users2` FOREIGN KEY (`owner`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `weapon`
--

DROP TABLE IF EXISTS `weapon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weapon` (
                          `implementierung` varchar(31) NOT NULL,
                          `id` varchar(255) NOT NULL,
                          `apCost` int NOT NULL,
                          `areaDamage` int NOT NULL,
                          `baseDamage` int NOT NULL,
                          `defSmallTrefferWS` int NOT NULL,
                          `defSubWS` int NOT NULL,
                          `defTorpTrefferWS` double NOT NULL,
                          `defTrefferWS` int NOT NULL,
                          `destroyable` tinyint(1) NOT NULL,
                          `eCost` int NOT NULL,
                          `name` varchar(255) DEFAULT NULL,
                          `shieldDamage` int NOT NULL,
                          `singleshots` int NOT NULL,
                          `subDamage` int NOT NULL,
                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `weapon_flags`
--

DROP TABLE IF EXISTS `weapon_flags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weapon_flags` (
                                `Weapon_id` varchar(255) NOT NULL,
                                `flags` int DEFAULT NULL,
                                KEY `weapon_flags_fk_weapon` (`Weapon_id`),
                                CONSTRAINT `weapon_flags_fk_weapon` FOREIGN KEY (`Weapon_id`) REFERENCES `weapon` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `weapon_munition`
--

DROP TABLE IF EXISTS `weapon_munition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `weapon_munition` (
                                   `Weapon_id` varchar(255) NOT NULL,
                                   `munition` varchar(255) DEFAULT NULL,
                                   KEY `weapon_munition_fk_weapon` (`Weapon_id`),
                                   CONSTRAINT `weapon_munition_fk_weapon` FOREIGN KEY (`Weapon_id`) REFERENCES `weapon` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `werft_queues`
--

DROP TABLE IF EXISTS `werft_queues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `werft_queues` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `flagschiff` tinyint(1) NOT NULL,
                                `item` int NOT NULL,
                                `costsPerTick` longtext NOT NULL,
                                `energyPerTick` int NOT NULL,
                                `position` int NOT NULL,
                                `remaining` int NOT NULL,
                                `scheduled` tinyint(1) NOT NULL,
                                `slots` int NOT NULL,
                                `building` int NOT NULL,
                                `werft` int NOT NULL,
                                PRIMARY KEY (`id`),
                                KEY `werft_queues_fk_ship_types` (`building`),
                                KEY `werft_queues_fk_werften` (`werft`),
                                CONSTRAINT `werft_queues_fk_ship_types` FOREIGN KEY (`building`) REFERENCES `ship_types` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                                CONSTRAINT `werft_queues_fk_werften` FOREIGN KEY (`werft`) REFERENCES `werften` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=901563 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `werften`
--

DROP TABLE IF EXISTS `werften`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `werften` (
                           `werfttype` char(1) NOT NULL,
                           `id` int NOT NULL AUTO_INCREMENT,
                           `flagschiff` tinyint(1) NOT NULL,
                           `type` int NOT NULL,
                           `version` int NOT NULL,
                           `komplex` tinyint(1) DEFAULT NULL,
                           `linkedWerft` int DEFAULT NULL,
                           `linked` int DEFAULT NULL,
                           `shipid` int DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           KEY `werften_fk_werften` (`linkedWerft`),
                           KEY `werften_fk_ships` (`shipid`),
                           KEY `werften_fk_bases` (`linked`),
                           CONSTRAINT `werften_fk_bases` FOREIGN KEY (`linked`) REFERENCES `bases` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT `werften_fk_ships` FOREIGN KEY (`shipid`) REFERENCES `ships` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT `werften_fk_werften` FOREIGN KEY (`linkedWerft`) REFERENCES `werften` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=34278 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-07-15 14:59:11

-- Berechnet die naechste freie Schiffs-ID ab der angegebenen ID
DROP FUNCTION IF EXISTS newIntelliShipID;
DELIMITER //
CREATE FUNCTION newIntelliShipID(minid INT) RETURNS INT
    READS SQL DATA
BEGIN
  DECLARE done,sid,shouldId INT DEFAULT 0;
  DECLARE cur1 CURSOR FOR SELECT DISTINCT abs(id) iid FROM ships WHERE abs(id)>=minid ORDER BY iid;
DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;

OPEN cur1;
set shouldId = minid;
  REPEAT
FETCH cur1 INTO sid;
    IF NOT done THEN
      IF sid <> shouldId THEN
        CLOSE cur1;
RETURN shouldId;
END IF;
      set shouldId = shouldId+1;
END IF;
  UNTIL done END REPEAT;

CLOSE cur1;
RETURN shouldId;
END;
//