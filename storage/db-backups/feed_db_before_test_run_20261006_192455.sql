-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: feed_db
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `data_migrations`
--

DROP TABLE IF EXISTS `data_migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_migrations` (
  `id` varchar(100) NOT NULL,
  `applied_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data_migrations`
--

LOCK TABLES `data_migrations` WRITE;
/*!40000 ALTER TABLE `data_migrations` DISABLE KEYS */;
INSERT INTO `data_migrations` VALUES ('epm-categories-seed-v1','2026-09-26 12:35:55.829313'),('epm-gallery-import-v1','2026-09-26 12:35:58.028297'),('epm-participant-type-links-v1','2026-10-02 10:58:31.113045'),('epm-reviews-seed-v1','2026-09-26 12:35:56.110429'),('epm-venues-seed-v1','2026-09-26 12:35:56.085498'),('system-admins-retire-user-admins-v1','2026-10-06 08:10:00.049301'),('system-admins-seed-v1','2026-10-06 08:10:00.007639'),('user-foreign-keys-v1','2026-10-02 10:58:31.637232'),('user-types-epm-participants-v1','2026-10-02 10:58:30.630444'),('user-types-seed-v1','2026-09-30 11:28:16.486610');
/*!40000 ALTER TABLE `data_migrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_admin_activities`
--

DROP TABLE IF EXISTS `epm_admin_activities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_admin_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(20) NOT NULL,
  `admin_id` bigint NOT NULL,
  `admin_username` varchar(30) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `epm_event_id` bigint NOT NULL,
  `event_city` varchar(255) DEFAULT NULL,
  `event_date` date NOT NULL,
  `event_title` varchar(255) NOT NULL,
  `field` varchar(20) DEFAULT NULL,
  `new_value` text,
  `old_value` text,
  PRIMARY KEY (`id`),
  KEY `idx_epm_admin_activities_created` (`created_at`),
  KEY `idx_epm_admin_activities_admin` (`admin_id`),
  KEY `idx_epm_admin_activities_event_date` (`event_date`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_admin_activities`
--

LOCK TABLES `epm_admin_activities` WRITE;
/*!40000 ALTER TABLE `epm_admin_activities` DISABLE KEYS */;
INSERT INTO `epm_admin_activities` VALUES (1,'CREATED',1,'admin','2026-10-06 11:08:01.788400',199,'vijayawada','2026-10-07','epm meeting for farmers awarness',NULL,NULL,NULL),(2,'UPDATED',3,'anand','2026-10-06 11:18:19.602818',199,'vijayawada','2026-10-07','epm meeting for farmers awarness','VENUE','y convention','c convention'),(3,'UPDATED',1,'admin','2026-10-06 11:49:59.450942',199,'vijayawada','2026-10-07','epm meeting for farmers awarness','VENUE','AB convention','y convention'),(7,'UPDATED',1,'admin','2026-10-06 13:20:29.502173',199,'vijayawada','2026-10-08','epm meeting for farmers awarness','DATE','2026-10-08','2026-10-07'),(8,'UPDATED',1,'admin','2026-10-06 13:48:12.737237',199,'vijayawada','2026-10-09','epm meeting for farmers awarness','DATE','2026-10-09','2026-10-08');
/*!40000 ALTER TABLE `epm_admin_activities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_categories`
--

DROP TABLE IF EXISTS `epm_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `color` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `label` varchar(200) NOT NULL,
  `name` varchar(100) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKoiisf3kaukykmu03t7r7ndfw9` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_categories`
--

LOCK TABLES `epm_categories` WRITE;
/*!40000 ALTER TABLE `epm_categories` DISABLE KEYS */;
INSERT INTO `epm_categories` VALUES (1,'green','2026-09-26 12:35:55.784525',0,'Good Agricultural Practices (GAP Workshop)','GAP Workshop','2026-09-26 12:35:55.784525'),(2,'teal','2026-09-26 12:35:55.813457',1,'Capacity Building Trainings','Capacity Building Trainings','2026-09-26 12:35:55.813457'),(3,'purple','2026-09-26 12:35:55.815478',2,'FPO Management Sessions','FPO Management Sessions','2026-09-26 12:35:55.815478'),(4,'blue','2026-09-26 12:35:55.816746',3,'EPM Meeting','EPM Meeting','2026-09-26 12:35:55.816746'),(5,'orange','2026-09-26 12:35:55.819075',4,'Export Workshops','Export Workshops','2026-09-26 12:35:55.819075');
/*!40000 ALTER TABLE `epm_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_event_updates`
--

DROP TABLE IF EXISTS `epm_event_updates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_event_updates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `epm_event_id` bigint NOT NULL,
  `field` varchar(20) NOT NULL,
  `new_value` text,
  `old_value` text,
  PRIMARY KEY (`id`),
  KEY `idx_epm_event_updates_event` (`epm_event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_event_updates`
--

LOCK TABLES `epm_event_updates` WRITE;
/*!40000 ALTER TABLE `epm_event_updates` DISABLE KEYS */;
INSERT INTO `epm_event_updates` VALUES (6,'2026-10-06 11:18:19.600451',199,'VENUE','y convention','c convention'),(7,'2026-10-06 11:49:59.434044',199,'VENUE','AB convention','y convention'),(9,'2026-10-06 13:20:29.451288',199,'DATE','2026-10-08','2026-10-07'),(10,'2026-10-06 13:48:12.672313',199,'DATE','2026-10-09','2026-10-08');
/*!40000 ALTER TABLE `epm_event_updates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_events`
--

DROP TABLE IF EXISTS `epm_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cancelled` bit(1) NOT NULL,
  `category` varchar(255) DEFAULT NULL,
  `city` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `district` varchar(255) NOT NULL,
  `event_date` date NOT NULL,
  `state` varchar(255) NOT NULL,
  `time_range` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `venue` varchar(255) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_events`
--

LOCK TABLES `epm_events` WRITE;
/*!40000 ALTER TABLE `epm_events` DISABLE KEYS */;
INSERT INTO `epm_events` VALUES (199,_binary '\0','EPM Meeting','vijayawada','2026-10-06 11:08:01.761060','krishna','2026-10-09','andhra',NULL,'epm meeting for farmers awarness','2026-10-06 13:48:12.756404','AB convention',NULL);
/*!40000 ALTER TABLE `epm_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_gallery_districts`
--

DROP TABLE IF EXISTS `epm_gallery_districts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_gallery_districts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `folder` varchar(120) NOT NULL,
  `name` varchar(120) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `state_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_epm_gallery_districts_state_slug` (`state_id`,`slug`),
  UNIQUE KEY `uk_epm_gallery_districts_state_folder` (`state_id`,`folder`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_gallery_districts`
--

LOCK TABLES `epm_gallery_districts` WRITE;
/*!40000 ALTER TABLE `epm_gallery_districts` DISABLE KEYS */;
INSERT INTO `epm_gallery_districts` VALUES (1,'2026-09-26 12:35:56.511137',0,'anantapur','Anantapur','anantapur',1,'2026-09-26 12:35:56.511137'),(2,'2026-09-26 12:35:56.622671',1,'east-godavari','East Godavari','east-godavari',1,'2026-09-26 12:35:56.622671'),(3,'2026-09-26 12:35:56.719470',2,'guntur','Guntur','guntur',1,'2026-09-26 12:35:56.719470'),(4,'2026-09-26 12:35:56.807853',3,'kakinada','Kakinada','kakinada',1,'2026-09-26 12:35:56.807853'),(5,'2026-09-26 12:35:56.901813',4,'krishna','Krishna','krishna',1,'2026-09-26 12:35:56.901813'),(6,'2026-09-26 12:35:56.976686',5,'kurnool','Kurnool','kurnool',1,'2026-09-26 12:35:56.976686'),(7,'2026-09-26 12:35:57.049140',6,'nellore','Nellore','nellore',1,'2026-09-26 12:35:57.049140'),(8,'2026-09-26 12:35:57.137762',7,'tirupati','Tirupati','tirupati',1,'2026-09-26 12:35:57.137762'),(9,'2026-09-26 12:35:57.209791',8,'visakhapatnam','Visakhapatnam','visakhapatnam',1,'2026-09-26 12:35:57.209791'),(10,'2026-09-26 12:35:57.314571',9,'west-godavari','West Godavari','west-godavari',1,'2026-09-26 12:35:57.314571'),(11,'2026-09-26 12:35:57.393613',0,'adilabad','Adilabad','adilabad',2,'2026-09-26 12:35:57.393613'),(12,'2026-09-26 12:35:57.455871',1,'hyderabad','Hyderabad','hyderabad',2,'2026-09-26 12:35:57.455871'),(13,'2026-09-26 12:35:57.544391',2,'karimnagar','Karimnagar','karimnagar',2,'2026-09-26 12:35:57.544391'),(14,'2026-09-26 12:35:57.609745',3,'khammam','Khammam','khammam',2,'2026-09-26 12:35:57.609745'),(15,'2026-09-26 12:35:57.670463',4,'mahabubnagar','Mahabubnagar','mahabubnagar',2,'2026-09-26 12:35:57.670463'),(16,'2026-09-26 12:35:57.724472',5,'nalgonda','Nalgonda','nalgonda',2,'2026-09-26 12:35:57.724472'),(17,'2026-09-26 12:35:57.788328',6,'nizamabad','Nizamabad','nizamabad',2,'2026-09-26 12:35:57.788328'),(18,'2026-09-26 12:35:57.853274',7,'rangareddy','Rangareddy','rangareddy',2,'2026-09-26 12:35:57.853274'),(19,'2026-09-26 12:35:57.916376',8,'sangareddy','Sangareddy','sangareddy',2,'2026-09-26 12:35:57.916376'),(20,'2026-09-26 12:35:57.969872',9,'warangal','Warangal','warangal',2,'2026-09-26 12:35:57.969872');
/*!40000 ALTER TABLE `epm_gallery_districts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_gallery_images`
--

DROP TABLE IF EXISTS `epm_gallery_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_gallery_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `block` varchar(64) NOT NULL,
  `caption` varchar(500) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `content_type` varchar(64) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `district_id` bigint DEFAULT NULL,
  `featured` bit(1) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `height` int DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `width` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_epm_gallery_images_block` (`block`),
  KEY `idx_epm_gallery_images_district` (`district_id`)
) ENGINE=InnoDB AUTO_INCREMENT=167 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_gallery_images`
--

LOCK TABLES `epm_gallery_images` WRITE;
/*!40000 ALTER TABLE `epm_gallery_images` DISABLE KEYS */;
INSERT INTO `epm_gallery_images` VALUES (1,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 11:00:00.000000',0,NULL,_binary '\0','agricultural_landscape_bottom.avif',82690,768,NULL,'2026-09-26 12:35:56.171409',1376),(2,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 11:00:00.000000',2,NULL,_binary '\0','epm_conference_hall.avif',120385,1024,NULL,'2026-09-26 12:35:56.182648',1024),(3,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 11:00:00.000000',1,NULL,_binary '\0','paddy_fields.avif',148851,768,NULL,'2026-09-26 12:35:56.192676',1376),(4,'epm-moments',NULL,NULL,'image/avif','2026-09-26 06:14:10.057870',0,NULL,_binary '\0','01_join.avif',106419,1024,NULL,'2026-09-26 12:35:56.202185',1024),(5,'epm-moments',NULL,NULL,'image/avif','2026-09-26 06:14:10.058869',0,NULL,_binary '\0','02_assess.avif',136960,1024,NULL,'2026-09-26 12:35:56.212296',1024),(6,'epm-moments',NULL,NULL,'image/avif','2026-09-26 06:14:10.059870',0,NULL,_binary '\0','03_grow.avif',210169,1024,NULL,'2026-09-26 12:35:56.221015',1024),(7,'epm-across-cities',NULL,NULL,'image/avif','2026-09-26 06:14:10.052870',0,NULL,_binary '\0','farmer_1.avif',69891,1024,NULL,'2026-09-26 12:35:56.232532',1024),(8,'epm-across-cities',NULL,NULL,'image/avif','2026-09-26 06:14:10.053871',0,NULL,_binary '\0','farmer_2.avif',83867,1024,NULL,'2026-09-26 12:35:56.241046',1024),(9,'epm-across-cities',NULL,NULL,'image/avif','2026-09-26 06:14:10.053871',0,NULL,_binary '\0','farmer_3.avif',65523,1024,NULL,'2026-09-26 12:35:56.249563',1024),(10,'inside-the-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.064104',0,NULL,_binary '\0','cost_reduction.avif',170224,896,NULL,'2026-09-26 12:35:56.259258',1200),(11,'inside-the-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.064104',0,NULL,_binary '\0','finance_credit.avif',87195,896,NULL,'2026-09-26 12:35:56.267775',1200),(12,'inside-the-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.065115',0,NULL,_binary '\0','technology_traceability.avif',130285,896,NULL,'2026-09-26 12:35:56.277461',1200),(13,'people-at-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.067119',0,NULL,_binary '\0','1.avif',125976,941,NULL,'2026-09-26 12:35:56.288136',1672),(14,'people-at-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.070118',0,NULL,_binary '\0','2.avif',858242,1536,NULL,'2026-09-26 12:35:56.296143',2816),(15,'people-at-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.070118',0,NULL,_binary '\0','3.avif',134144,941,NULL,'2026-09-26 12:35:56.305663',1672),(16,'connections-at-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.049855',0,NULL,_binary '\0','asset_leasing.avif',158022,896,NULL,'2026-09-26 12:35:56.321236',1200),(17,'connections-at-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.050869',0,NULL,_binary '\0','field_background.avif',125804,768,NULL,'2026-09-26 12:35:56.335097',1376),(18,'connections-at-epm',NULL,NULL,'image/avif','2026-09-26 06:14:10.051870',0,NULL,_binary '\0','warehouse_finance.avif',116518,896,NULL,'2026-09-26 12:35:56.351179',1200),(19,'event-details',NULL,NULL,'image/avif','2026-09-26 06:14:10.060872',0,NULL,_binary '\0','farmer_kiran.avif',87163,1024,NULL,'2026-09-26 12:35:56.368133',1024),(20,'event-details',NULL,NULL,'image/avif','2026-09-26 06:14:10.061875',0,NULL,_binary '\0','farmer_lakshmi.avif',90260,1024,NULL,'2026-09-26 12:35:56.381906',1024),(21,'event-details',NULL,NULL,'image/avif','2026-09-26 06:14:10.061875',0,NULL,_binary '\0','farmer_ramesh.avif',91595,1024,NULL,'2026-09-26 12:35:56.392934',1024),(22,'the-epm-experience',NULL,NULL,'image/avif','2026-09-26 06:14:10.072118',0,NULL,_binary '\0','04_add_value.avif',163132,1024,NULL,'2026-09-26 12:35:56.406978',1024),(23,'the-epm-experience',NULL,NULL,'image/avif','2026-09-26 06:14:10.072118',0,NULL,_binary '\0','05_go_to_market.avif',132141,896,NULL,'2026-09-26 12:35:56.419225',1200),(24,'the-epm-experience',NULL,NULL,'image/avif','2026-09-26 06:14:10.073118',0,NULL,_binary '\0','agri_bg.avif',149366,1024,NULL,'2026-09-26 12:35:56.435010',1024),(25,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.395611',0,1,_binary '\0','01.avif',82690,768,NULL,'2026-09-26 12:35:56.530504',1376),(26,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.395611',1,1,_binary '\0','02.avif',180418,896,NULL,'2026-09-26 12:35:56.541839',1200),(27,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.412155',2,1,_binary '\0','03.avif',170224,896,NULL,'2026-09-26 12:35:56.552561',1200),(28,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.412155',3,1,_binary '\0','04.avif',116518,896,NULL,'2026-09-26 12:35:56.566736',1200),(29,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.412155',4,1,_binary '\0','05.avif',126722,1024,NULL,'2026-09-26 12:35:56.580345',1024),(30,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.412155',5,1,_binary '\0','06.avif',132141,896,NULL,'2026-09-26 12:35:56.593753',1200),(31,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.412155',6,1,_binary '\0','07.avif',163132,1024,NULL,'2026-09-26 12:35:56.608328',1024),(32,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.412155',0,2,_binary '\0','01.avif',148851,768,NULL,'2026-09-26 12:35:56.637564',1376),(33,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.423155',1,2,_binary '\0','02.avif',116916,1024,NULL,'2026-09-26 12:35:56.647659',1024),(34,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.425932',2,2,_binary '\0','03.avif',214334,896,NULL,'2026-09-26 12:35:56.658529',1200),(35,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.426946',3,2,_binary '\0','04.avif',175992,768,NULL,'2026-09-26 12:35:56.668052',1376),(36,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.428747',4,2,_binary '\0','05.avif',106419,1024,NULL,'2026-09-26 12:35:56.677569',1024),(37,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.428747',5,2,_binary '\0','06.avif',149366,1024,NULL,'2026-09-26 12:35:56.688607',1024),(38,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.433149',6,2,_binary '\0','07.avif',210169,1024,NULL,'2026-09-26 12:35:56.698190',1024),(39,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.434410',7,2,_binary '\0','08.avif',138000,1024,NULL,'2026-09-26 12:35:56.706812',1024),(40,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.436973',0,3,_binary '\0','01.avif',180418,896,NULL,'2026-09-26 12:35:56.734940',1200),(41,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.438610',1,3,_binary '\0','02.avif',97358,768,NULL,'2026-09-26 12:35:56.745881',1376),(42,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.440632',2,3,_binary '\0','03.avif',175803,1123,NULL,'2026-09-26 12:35:56.756310',1401),(43,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.440632',3,3,_binary '\0','04.avif',158022,896,NULL,'2026-09-26 12:35:56.767326',1200),(44,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.444264',4,3,_binary '\0','05.avif',198788,916,NULL,'2026-09-26 12:35:56.775848',1717),(45,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.444923',5,3,_binary '\0','06.avif',136960,1024,NULL,'2026-09-26 12:35:56.786492',1024),(46,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.444923',6,3,_binary '\0','07.avif',253189,941,NULL,'2026-09-26 12:35:56.796526',1672),(47,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.448767',0,4,_binary '\0','01.avif',112642,768,NULL,'2026-09-26 12:35:56.847672',1376),(48,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.451241',1,4,_binary '\0','02.avif',98222,562,NULL,'2026-09-26 12:35:56.857204',1327),(49,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.452856',2,4,_binary '\0','03.avif',234476,1677,NULL,'2026-09-26 12:35:56.867114',938),(50,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.455238',3,4,_binary '\0','04.avif',131487,1024,NULL,'2026-09-26 12:35:56.876975',1024),(51,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.455238',4,4,_binary '\0','05.avif',118080,1024,NULL,'2026-09-26 12:35:56.884489',1024),(52,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.459616',5,4,_binary '\0','06.avif',125804,768,NULL,'2026-09-26 12:35:56.893107',1376),(53,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.461757',0,5,_binary '\0','01.avif',149366,1024,NULL,'2026-09-26 12:35:56.911333',1024),(54,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.461757',1,5,_binary '\0','02.avif',148851,768,NULL,'2026-09-26 12:35:56.919853',1376),(55,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.464440',2,5,_binary '\0','03.avif',225092,1122,NULL,'2026-09-26 12:35:56.928366',1402),(56,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.467095',3,5,_binary '\0','04.avif',160208,1024,NULL,'2026-09-26 12:35:56.936372',1536),(57,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.467602',4,5,_binary '\0','05.avif',87195,896,NULL,'2026-09-26 12:35:56.946340',1200),(58,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.469984',5,5,_binary '\0','06.avif',132141,896,NULL,'2026-09-26 12:35:56.956650',1200),(59,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.472010',6,5,_binary '\0','07.avif',163855,896,NULL,'2026-09-26 12:35:56.966168',1200),(60,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.473172',0,6,_binary '\0','01.avif',158022,896,NULL,'2026-09-26 12:35:56.987713',1200),(61,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.475872',1,6,_binary '\0','02.avif',103974,1024,NULL,'2026-09-26 12:35:56.997504',1024),(62,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.475872',2,6,_binary '\0','03.avif',123856,1024,NULL,'2026-09-26 12:35:57.008024',1024),(63,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.475872',3,6,_binary '\0','04.avif',82690,768,NULL,'2026-09-26 12:35:57.017795',1376),(64,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.481015',4,6,_binary '\0','05.avif',130285,896,NULL,'2026-09-26 12:35:57.028809',1200),(65,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.482344',5,6,_binary '\0','06.avif',180418,896,NULL,'2026-09-26 12:35:57.038617',1200),(66,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.485211',0,7,_binary '\0','01.avif',214334,896,NULL,'2026-09-26 12:35:57.061662',1200),(67,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.486467',1,7,_binary '\0','02.avif',126722,1024,NULL,'2026-09-26 12:35:57.071124',1024),(68,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',2,7,_binary '\0','03.avif',132141,896,NULL,'2026-09-26 12:35:57.082752',1200),(69,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',3,7,_binary '\0','04.avif',163132,1024,NULL,'2026-09-26 12:35:57.091792',1024),(70,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',4,7,_binary '\0','05.avif',112642,768,NULL,'2026-09-26 12:35:57.101265',1376),(71,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',5,7,_binary '\0','06.avif',116916,1024,NULL,'2026-09-26 12:35:57.110532',1024),(72,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',6,7,_binary '\0','07.avif',175992,768,NULL,'2026-09-26 12:35:57.119248',1376),(73,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',7,7,_binary '\0','08.avif',106419,1024,NULL,'2026-09-26 12:35:57.128264',1024),(74,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.487476',0,8,_binary '\0','01.avif',225092,1122,NULL,'2026-09-26 12:35:57.148579',1402),(75,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.499212',1,8,_binary '\0','02.avif',106419,1024,NULL,'2026-09-26 12:35:57.158104',1024),(76,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.500439',2,8,_binary '\0','03.avif',149366,1024,NULL,'2026-09-26 12:35:57.166708',1024),(77,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.500439',3,8,_binary '\0','04.avif',210169,1024,NULL,'2026-09-26 12:35:57.175580',1024),(78,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.502943',4,8,_binary '\0','05.avif',138000,1024,NULL,'2026-09-26 12:35:57.184234',1024),(79,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.502943',5,8,_binary '\0','06.avif',110268,1024,NULL,'2026-09-26 12:35:57.191752',1024),(80,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.505641',6,8,_binary '\0','07.avif',97358,768,NULL,'2026-09-26 12:35:57.200270',1376),(81,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.508315',0,9,_binary '\0','01.avif',126722,1024,NULL,'2026-09-26 12:35:57.220545',1024),(82,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.508315',1,9,_binary '\0','02.avif',198788,916,NULL,'2026-09-26 12:35:57.232079',1717),(83,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.508315',2,9,_binary '\0','03.avif',136960,1024,NULL,'2026-09-26 12:35:57.242917',1024),(84,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.508315',3,9,_binary '\0','04.avif',253189,941,NULL,'2026-09-26 12:35:57.252478',1672),(85,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.508315',4,9,_binary '\0','05.avif',116656,768,NULL,'2026-09-26 12:35:57.265005',1376),(86,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.515175',5,9,_binary '\0','06.avif',98222,562,NULL,'2026-09-26 12:35:57.277854',1327),(87,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.516900',6,9,_binary '\0','07.avif',234476,1677,NULL,'2026-09-26 12:35:57.288717',938),(88,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.516900',7,9,_binary '\0','08.avif',131487,1024,NULL,'2026-09-26 12:35:57.298529',1024),(89,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.516900',8,9,_binary '\0','09.avif',118080,1024,NULL,'2026-09-26 12:35:57.307050',1024),(90,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.516900',0,10,_binary '\0','01.avif',253189,941,NULL,'2026-09-26 12:35:57.324090',1672),(91,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.523179',1,10,_binary '\0','02.avif',118080,1024,NULL,'2026-09-26 12:35:57.332675',1024),(92,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.525121',2,10,_binary '\0','03.avif',125804,768,NULL,'2026-09-26 12:35:57.341370',1376),(93,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.525121',3,10,_binary '\0','04.avif',110062,768,NULL,'2026-09-26 12:35:57.350527',1376),(94,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.525121',4,10,_binary '\0','05.avif',158022,896,NULL,'2026-09-26 12:35:57.359124',1200),(95,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.525121',5,10,_binary '\0','06.avif',148851,768,NULL,'2026-09-26 12:35:57.365640',1376),(96,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.531118',0,11,_binary '\0','01.avif',125804,768,NULL,'2026-09-26 12:35:57.402125',1376),(97,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.531118',1,11,_binary '\0','02.avif',87195,896,NULL,'2026-09-26 12:35:57.410645',1200),(98,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.534795',2,11,_binary '\0','03.avif',132141,896,NULL,'2026-09-26 12:35:57.418150',1200),(99,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.535926',3,11,_binary '\0','04.avif',163855,896,NULL,'2026-09-26 12:35:57.426477',1200),(100,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.535926',4,11,_binary '\0','05.avif',120385,1024,NULL,'2026-09-26 12:35:57.433898',1024),(101,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.535926',5,11,_binary '\0','06.avif',103974,1024,NULL,'2026-09-26 12:35:57.439963',1024),(102,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.535926',6,11,_binary '\0','07.avif',123856,1024,NULL,'2026-09-26 12:35:57.448686',1024),(103,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.541931',0,12,_binary '\0','01.avif',120385,1024,NULL,'2026-09-26 12:35:57.465426',1024),(104,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.544143',1,12,_binary '\0','02.avif',130285,896,NULL,'2026-09-26 12:35:57.473946',1200),(105,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.544143',2,12,_binary '\0','03.avif',180418,896,NULL,'2026-09-26 12:35:57.482455',1200),(106,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.547170',3,12,_binary '\0','04.avif',170224,896,NULL,'2026-09-26 12:35:57.490983',1200),(107,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.548876',4,12,_binary '\0','05.avif',116518,896,NULL,'2026-09-26 12:35:57.498898',1200),(108,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.548876',5,12,_binary '\0','06.avif',126722,1024,NULL,'2026-09-26 12:35:57.507485',1024),(109,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.551747',6,12,_binary '\0','07.avif',132141,896,NULL,'2026-09-26 12:35:57.515819',1200),(110,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.551747',7,12,_binary '\0','08.avif',163132,1024,NULL,'2026-09-26 12:35:57.526510',1024),(111,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.553693',8,12,_binary '\0','09.avif',112642,768,NULL,'2026-09-26 12:35:57.534870',1376),(112,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.553693',0,13,_binary '\0','01.avif',130285,896,NULL,'2026-09-26 12:35:57.553443',1200),(113,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.553693',1,13,_binary '\0','02.avif',112642,768,NULL,'2026-09-26 12:35:57.564037',1376),(114,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.559465',2,13,_binary '\0','03.avif',116916,1024,NULL,'2026-09-26 12:35:57.573535',1024),(115,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.559972',3,13,_binary '\0','04.avif',214334,896,NULL,'2026-09-26 12:35:57.583662',1200),(116,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.561979',4,13,_binary '\0','05.avif',175992,768,NULL,'2026-09-26 12:35:57.594095',1376),(117,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.562986',5,13,_binary '\0','06.avif',106419,1024,NULL,'2026-09-26 12:35:57.602383',1024),(118,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.564995',0,14,_binary '\0','01.avif',210169,1024,NULL,'2026-09-26 12:35:57.617911',1024),(119,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.567000',1,14,_binary '\0','02.avif',110268,1024,NULL,'2026-09-26 12:35:57.624429',1024),(120,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.567000',2,14,_binary '\0','03.avif',97358,768,NULL,'2026-09-26 12:35:57.632441',1376),(121,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.567000',3,14,_binary '\0','04.avif',175803,1123,NULL,'2026-09-26 12:35:57.640352',1401),(122,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.571532',4,14,_binary '\0','05.avif',158022,896,NULL,'2026-09-26 12:35:57.647422',1200),(123,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.571532',5,14,_binary '\0','06.avif',198788,916,NULL,'2026-09-26 12:35:57.654939',1717),(124,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.571532',6,14,_binary '\0','07.avif',136960,1024,NULL,'2026-09-26 12:35:57.663447',1024),(125,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.576186',0,15,_binary '\0','01.avif',106419,1024,NULL,'2026-09-26 12:35:57.678985',1024),(126,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.578873',1,15,_binary '\0','02.avif',116656,768,NULL,'2026-09-26 12:35:57.688006',1376),(127,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.580082',2,15,_binary '\0','03.avif',98222,562,NULL,'2026-09-26 12:35:57.695013',1327),(128,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.580082',3,15,_binary '\0','04.avif',234476,1677,NULL,'2026-09-26 12:35:57.703112',938),(129,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.582092',4,15,_binary '\0','05.avif',131487,1024,NULL,'2026-09-26 12:35:57.709952',1024),(130,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.582092',5,15,_binary '\0','06.avif',118080,1024,NULL,'2026-09-26 12:35:57.716459',1024),(131,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.585414',0,16,_binary '\0','01.avif',136960,1024,NULL,'2026-09-26 12:35:57.733995',1024),(132,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.586923',1,16,_binary '\0','02.avif',158022,896,NULL,'2026-09-26 12:35:57.740509',1200),(133,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.589006',2,16,_binary '\0','03.avif',148851,768,NULL,'2026-09-26 12:35:57.748510',1376),(134,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.589006',3,16,_binary '\0','04.avif',225092,1122,NULL,'2026-09-26 12:35:57.755024',1402),(135,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.589006',4,16,_binary '\0','05.avif',160208,1024,NULL,'2026-09-26 12:35:57.761540',1536),(136,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.589006',5,16,_binary '\0','06.avif',87195,896,NULL,'2026-09-26 12:35:57.768540',1200),(137,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.594882',6,16,_binary '\0','07.avif',132141,896,NULL,'2026-09-26 12:35:57.775055',1200),(138,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.594882',7,16,_binary '\0','08.avif',163855,896,NULL,'2026-09-26 12:35:57.782569',1200),(139,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.598853',0,17,_binary '\0','01.avif',175803,1123,NULL,'2026-09-26 12:35:57.797459',1401),(140,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.598853',1,17,_binary '\0','02.avif',120385,1024,NULL,'2026-09-26 12:35:57.804975',1024),(141,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.598853',2,17,_binary '\0','03.avif',103974,1024,NULL,'2026-09-26 12:35:57.812411',1024),(142,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.598853',3,17,_binary '\0','04.avif',123856,1024,NULL,'2026-09-26 12:35:57.819499',1024),(143,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.598853',4,17,_binary '\0','05.avif',82690,768,NULL,'2026-09-26 12:35:57.826020',1376),(144,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.606341',5,17,_binary '\0','06.avif',130285,896,NULL,'2026-09-26 12:35:57.833902',1200),(145,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.606341',6,17,_binary '\0','07.avif',180418,896,NULL,'2026-09-26 12:35:57.841750',1200),(146,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.606341',0,18,_binary '\0','01.avif',103974,1024,NULL,'2026-09-26 12:35:57.860787',1024),(147,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.610735',1,18,_binary '\0','02.avif',116518,896,NULL,'2026-09-26 12:35:57.868305',1200),(148,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.612696',2,18,_binary '\0','03.avif',126722,1024,NULL,'2026-09-26 12:35:57.874305',1024),(149,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.614481',3,18,_binary '\0','04.avif',132141,896,NULL,'2026-09-26 12:35:57.881824',1200),(150,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.615817',4,18,_binary '\0','05.avif',163132,1024,NULL,'2026-09-26 12:35:57.887857',1024),(151,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.615817',5,18,_binary '\0','06.avif',112642,768,NULL,'2026-09-26 12:35:57.894863',1376),(152,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.617934',6,18,_binary '\0','07.avif',116916,1024,NULL,'2026-09-26 12:35:57.902381',1024),(153,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.620148',7,18,_binary '\0','08.avif',214334,896,NULL,'2026-09-26 12:35:57.908573',1200),(154,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.621953',0,19,_binary '\0','01.avif',118080,1024,NULL,'2026-09-26 12:35:57.924428',1024),(155,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.623976',1,19,_binary '\0','02.avif',175992,768,NULL,'2026-09-26 12:35:57.931951',1376),(156,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.623976',2,19,_binary '\0','03.avif',106419,1024,NULL,'2026-09-26 12:35:57.939452',1024),(157,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.626745',3,19,_binary '\0','04.avif',149366,1024,NULL,'2026-09-26 12:35:57.946851',1024),(158,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.626745',4,19,_binary '\0','05.avif',210169,1024,NULL,'2026-09-26 12:35:57.954383',1024),(159,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.629844',5,19,_binary '\0','06.avif',138000,1024,NULL,'2026-09-26 12:35:57.961889',1024),(160,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.631976',0,20,_binary '\0','01.avif',116656,768,NULL,'2026-09-26 12:35:57.977830',1376),(161,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.633177',1,20,_binary '\0','02.avif',175803,1123,NULL,'2026-09-26 12:35:57.984510',1401),(162,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.633177',2,20,_binary '\0','03.avif',158022,896,NULL,'2026-09-26 12:35:57.991863',1200),(163,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.635914',3,20,_binary '\0','04.avif',198788,916,NULL,'2026-09-26 12:35:57.999996',1717),(164,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.635914',4,20,_binary '\0','05.avif',136960,1024,NULL,'2026-09-26 12:35:58.006513',1024),(165,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.637922',5,20,_binary '\0','06.avif',253189,941,NULL,'2026-09-26 12:35:58.015021',1672),(166,'epm-gallery',NULL,NULL,'image/avif','2026-09-26 06:52:13.637922',6,20,_binary '\0','07.avif',98222,562,NULL,'2026-09-26 12:35:58.022767',1327);
/*!40000 ALTER TABLE `epm_gallery_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_gallery_states`
--

DROP TABLE IF EXISTS `epm_gallery_states`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_gallery_states` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cover_file` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `folder` varchar(120) NOT NULL,
  `name` varchar(120) NOT NULL,
  `slug` varchar(120) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnixrtmlnd1wfa97ot2ecg2yvv` (`folder`),
  UNIQUE KEY `UKo5mkvko35odi4i2vsbbft3t3u` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_gallery_states`
--

LOCK TABLES `epm_gallery_states` WRITE;
/*!40000 ALTER TABLE `epm_gallery_states` DISABLE KEYS */;
INSERT INTO `epm_gallery_states` VALUES (1,'cover.avif','2026-09-26 12:35:56.462638',0,'andhra-pradesh','Andhra Pradesh','andhra-pradesh','2026-09-26 12:35:56.477691'),(2,'cover.avif','2026-09-26 12:35:57.373646',1,'telangana','Telangana','telangana','2026-09-26 12:35:57.382594');
/*!40000 ALTER TABLE `epm_gallery_states` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_page_videos`
--

DROP TABLE IF EXISTS `epm_page_videos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_page_videos` (
  `slot` varchar(64) NOT NULL,
  `content_type` varchar(64) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_page_videos`
--

LOCK TABLES `epm_page_videos` WRITE;
/*!40000 ALTER TABLE `epm_page_videos` DISABLE KEYS */;
INSERT INTO `epm_page_videos` VALUES ('epm-hero','video/mp4','2026-09-30 12:04:36.769887','epm-hero-e63e6183-f3f8-4ba2-ad56-287bec842a75.mp4',16455069,'2026-09-30 12:04:36.769887');
/*!40000 ALTER TABLE `epm_page_videos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_registrations`
--

DROP TABLE IF EXISTS `epm_registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_registrations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consent` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `district` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `epm_event_id` bigint NOT NULL,
  `event_city` varchar(255) NOT NULL,
  `event_date` date NOT NULL,
  `event_state` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `mobile_number` varchar(255) NOT NULL,
  `legacy_participant_type` varchar(255) DEFAULT NULL,
  `state` varchar(255) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `participant_type_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_epm_registrations_participant_type` (`participant_type_id`),
  KEY `fk_epm_registrations_user` (`user_id`),
  CONSTRAINT `fk_epm_registrations_participant_type` FOREIGN KEY (`participant_type_id`) REFERENCES `user_types` (`id`),
  CONSTRAINT `fk_epm_registrations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_registrations`
--

LOCK TABLES `epm_registrations` WRITE;
/*!40000 ALTER TABLE `epm_registrations` DISABLE KEYS */;
INSERT INTO `epm_registrations` VALUES (11,_binary '','2026-10-06 11:32:24.946566','Eluru','pavan.feed.dev@gmail.com',199,'vijayawada','2026-10-09','andhra','chikati pavan kalyan','9398821180',NULL,'Andhra Pradesh',1,1);
/*!40000 ALTER TABLE `epm_registrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_reviews`
--

DROP TABLE IF EXISTS `epm_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_name` varchar(255) NOT NULL,
  `author_role` varchar(255) DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `published` bit(1) NOT NULL,
  `rating` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_reviews`
--

LOCK TABLES `epm_reviews` WRITE;
/*!40000 ALTER TABLE `epm_reviews` DISABLE KEYS */;
INSERT INTO `epm_reviews` VALUES (1,'Ramesh','Farmer','Great initiative! EPM helped me understand export opportunities clearly.','2026-09-26 12:35:56.099741',0,_binary '',5,'2026-09-26 12:35:56.099741'),(2,'Sita','Entrepreneur','Informative session with practical insights on global markets.','2026-09-26 12:35:56.101195',1,_binary '',5,'2026-09-26 12:35:56.101195'),(3,'Anand','FPO Member','Well organized and very impactful meeting for our FPO members.','2026-09-26 12:35:56.102193',2,_binary '',5,'2026-09-26 12:35:56.102193'),(4,'Kiran','Trader','The networking opportunities were fantastic. Highly recommend attending.','2026-09-26 12:35:56.103390',3,_binary '',5,'2026-09-26 12:35:56.103390'),(5,'Lakshmi','Agri-Business','Excellent guidance on export documentation and compliance.','2026-09-26 12:35:56.104405',4,_binary '',5,'2026-09-26 12:35:56.104405');
/*!40000 ALTER TABLE `epm_reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_venues`
--

DROP TABLE IF EXISTS `epm_venues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_venues` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(500) DEFAULT NULL,
  `city` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `district` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=154 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_venues`
--

LOCK TABLES `epm_venues` WRITE;
/*!40000 ALTER TABLE `epm_venues` DISABLE KEYS */;
INSERT INTO `epm_venues` VALUES (151,NULL,'vijayawada','2026-10-06 11:08:01.788400','krishna','c convention','andhra','2026-10-06 11:08:01.788400'),(152,NULL,'vijayawada','2026-10-06 11:18:19.602818','krishna','y convention','andhra','2026-10-06 11:18:19.602818'),(153,NULL,'vijayawada','2026-10-06 11:49:59.448180','krishna','AB convention','andhra','2026-10-06 11:49:59.448180');
/*!40000 ALTER TABLE `epm_venues` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epm_volunteers`
--

DROP TABLE IF EXISTS `epm_volunteers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epm_volunteers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `district` varchar(255) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `epm_event_id` bigint NOT NULL,
  `event_city` varchar(255) NOT NULL,
  `event_date` date NOT NULL,
  `event_state` varchar(255) NOT NULL,
  `legacy_experience` varchar(255) DEFAULT NULL,
  `full_name` varchar(255) NOT NULL,
  `mobile_number` varchar(255) NOT NULL,
  `reason` text,
  `state` varchar(255) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `participant_type_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_epm_volunteers_participant_type` (`participant_type_id`),
  KEY `fk_epm_volunteers_user` (`user_id`),
  CONSTRAINT `fk_epm_volunteers_participant_type` FOREIGN KEY (`participant_type_id`) REFERENCES `user_types` (`id`),
  CONSTRAINT `fk_epm_volunteers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_volunteers`
--

LOCK TABLES `epm_volunteers` WRITE;
/*!40000 ALTER TABLE `epm_volunteers` DISABLE KEYS */;
/*!40000 ALTER TABLE `epm_volunteers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ers_assessments`
--

DROP TABLE IF EXISTS `ers_assessments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ers_assessments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answers_json` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `max_score` int NOT NULL,
  `passed` bit(1) NOT NULL,
  `percentage` double NOT NULL,
  `total_score` int NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_ers_assessments_user` (`user_id`),
  CONSTRAINT `fk_ers_assessments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ers_assessments`
--

LOCK TABLES `ers_assessments` WRITE;
/*!40000 ALTER TABLE `ers_assessments` DISABLE KEYS */;
INSERT INTO `ers_assessments` VALUES (1,'{\"testing\":1,\"training\":0,\"orgcert\":0,\"pricing\":0,\"volume\":0,\"storage\":0,\"gst\":0,\"ecgc\":0,\"wcap\":0,\"pkglabel\":0,\"buyer\":0,\"accounts\":1,\"iec\":2,\"docs\":1,\"pkgmaterial\":1,\"consistency\":1,\"entity\":2,\"barcode\":0,\"fssai\":0}','2026-08-22 12:58:46.295835',100,_binary '\0',9,9,6),(2,'{\"testing\":6,\"training\":2,\"orgcert\":8,\"pricing\":4,\"volume\":6,\"storage\":4,\"gst\":6,\"ecgc\":5,\"wcap\":6,\"pkglabel\":6,\"buyer\":6,\"accounts\":4,\"iec\":8,\"docs\":3,\"pkgmaterial\":5,\"consistency\":5,\"entity\":6,\"barcode\":4,\"fssai\":6}','2026-08-22 12:59:00.193139',100,_binary '',100,100,6),(3,'{\"testing\":0,\"training\":0,\"orgcert\":0,\"pricing\":0,\"volume\":0,\"storage\":0,\"gst\":0,\"ecgc\":0,\"wcap\":0,\"pkglabel\":0,\"buyer\":0,\"accounts\":0,\"iec\":8,\"docs\":0,\"pkgmaterial\":0,\"consistency\":0,\"entity\":0,\"barcode\":0,\"fssai\":0}','2026-08-22 12:59:00.401496',100,_binary '\0',8,8,6),(4,'{\"consistency\":3,\"pkgmaterial\":5,\"docs\":3,\"iec\":8,\"accounts\":1,\"buyer\":2,\"pkglabel\":4,\"wcap\":4,\"ecgc\":3,\"gst\":4,\"storage\":4,\"volume\":4,\"pricing\":4,\"orgcert\":5,\"training\":2,\"testing\":1,\"fssai\":2,\"barcode\":0,\"entity\":2}','2026-08-24 07:44:12.121376',100,_binary '\0',61,61,1),(5,'{\"pkgmaterial\":5,\"docs\":3,\"iec\":8,\"accounts\":2,\"buyer\":6,\"pkglabel\":4,\"wcap\":6,\"ecgc\":5,\"gst\":6,\"storage\":2,\"volume\":4,\"pricing\":4,\"orgcert\":5,\"training\":2,\"testing\":6,\"fssai\":6,\"barcode\":4,\"entity\":6,\"consistency\":3}','2026-08-25 06:03:12.691858',100,_binary '',87,87,1);
/*!40000 ALTER TABLE `ers_assessments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `publications`
--

DROP TABLE IF EXISTS `publications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `publications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `file_size_bytes` bigint DEFAULT NULL,
  `month` int NOT NULL,
  `page_count` int DEFAULT NULL,
  `pdf_file` varchar(255) NOT NULL,
  `published_date` date DEFAULT NULL,
  `thumbnail_file` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `year` int NOT NULL,
  `issue_number` int DEFAULT NULL,
  `language` enum('English','Hindi','Telugu') NOT NULL DEFAULT 'English',
  `volume` int DEFAULT NULL,
  `order` int NOT NULL DEFAULT '3',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqxtw98ahdr1g9x6xwx0ck6qoh` (`year`,`month`),
  UNIQUE KEY `uk_publications_year_month_language` (`year`,`month`,`language`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `publications`
--

LOCK TABLES `publications` WRITE;
/*!40000 ALTER TABLE `publications` DISABLE KEYS */;
INSERT INTO `publications` VALUES (1,'2026-09-22 06:52:11.108654',4606192,9,19,'78770ce6-d8c2-4fd2-9338-47132f56a6df.pdf','2026-09-01','78770ce6-d8c2-4fd2-9338-47132f56a6df.png','FeedWorld ','2026-09-22 06:52:11.108654',2026,NULL,'English',NULL,3),(3,'2026-09-30 12:41:43.947756',NULL,6,19,'feed_world_telugu.pdf','2027-06-01','feed_world_telugu.png','Feed World','2026-09-30 12:41:43.947756',2027,NULL,'Telugu',NULL,1),(4,'2026-09-30 12:42:13.665205',NULL,7,19,'feed_world_telugu.pdf','2027-07-01','feed_world_telugu.png','Feed World','2026-09-30 12:42:13.665205',2027,NULL,'Telugu',NULL,1);
/*!40000 ALTER TABLE `publications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_admin_sessions`
--

DROP TABLE IF EXISTS `system_admin_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_admin_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `ip_address` varchar(64) DEFAULT NULL,
  `jti` varchar(64) NOT NULL,
  `last_seen_at` datetime(6) NOT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `admin_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKejjbi3s91qeu2xock1ano60jk` (`jti`),
  KEY `fk_system_admin_sessions_admin` (`admin_id`),
  CONSTRAINT `fk_system_admin_sessions_admin` FOREIGN KEY (`admin_id`) REFERENCES `system_admins` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_admin_sessions`
--

LOCK TABLES `system_admin_sessions` WRITE;
/*!40000 ALTER TABLE `system_admin_sessions` DISABLE KEYS */;
INSERT INTO `system_admin_sessions` VALUES (9,'2026-10-06 11:58:57.157122','0:0:0:0:0:0:0:1','8b5dc230-0cf6-4822-bed8-96139b588d0a','2026-10-06 11:58:57.157122','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Claude/2.19675.1 Chrome/152.0.7977.130 Safari/537.36 MSIX',1),(10,'2026-10-06 12:11:11.742707','0:0:0:0:0:0:0:1','47f0c58c-5569-428b-8fb4-f75075a3bd1c','2026-10-06 13:53:13.465162','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36',1),(11,'2026-10-06 13:14:04.024434','127.0.0.1','8224dadd-6a79-4dea-ab74-8772ed234e62','2026-10-06 13:14:04.024434','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Claude/2.19675.1 Chrome/152.0.7977.130 Safari/537.36 MSIX',1);
/*!40000 ALTER TABLE `system_admin_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_admins`
--

DROP TABLE IF EXISTS `system_admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_admins` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `mobile_number` varchar(10) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(30) NOT NULL,
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpmv8sg802pp28h6siimn8f2kd` (`email`),
  UNIQUE KEY `UKcw4r8av7yhfsb7as9q7bbgw4u` (`mobile_number`),
  UNIQUE KEY `UKqif4uugdvb49vvkntcsm1svw8` (`username`),
  KEY `fk_system_admins_created_by` (`created_by`),
  CONSTRAINT `fk_system_admins_created_by` FOREIGN KEY (`created_by`) REFERENCES `system_admins` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_admins`
--

LOCK TABLES `system_admins` WRITE;
/*!40000 ALTER TABLE `system_admins` DISABLE KEYS */;
INSERT INTO `system_admins` VALUES (1,'2026-10-06 08:09:59.960988','admin@feedworld.com','9642427354','$2a$12$XiQ8tYxgWYH8.ZjGk9zFiu38lJSn3CXjcUFR3YzHRyFz/H8X7twRK','admin',NULL),(3,'2026-10-06 09:02:11.185486','anand@123.com','6302129460','$2a$12$WNjVH2RGUjaymqrcdfAVYeMmq3v8HyrNw4J8vT2Q9cr4Bg1eLUhyS','anand',1);
/*!40000 ALTER TABLE `system_admins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_notification_state`
--

DROP TABLE IF EXISTS `user_notification_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_notification_state` (
  `user_id` bigint NOT NULL,
  `seen_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_notification_state_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_notification_state`
--

LOCK TABLES `user_notification_state` WRITE;
/*!40000 ALTER TABLE `user_notification_state` DISABLE KEYS */;
INSERT INTO `user_notification_state` VALUES (1,'2026-10-06 13:48:26.715868'),(19,'2026-10-03 06:04:15.579533');
/*!40000 ALTER TABLE `user_notification_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_sessions`
--

DROP TABLE IF EXISTS `user_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `ip_address` varchar(64) DEFAULT NULL,
  `jti` varchar(64) NOT NULL,
  `last_seen_at` datetime(6) NOT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKijeu37aypirrg8upfuhu1cds8` (`jti`),
  KEY `fk_user_sessions_user` (`user_id`),
  CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=92 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_sessions`
--

LOCK TABLES `user_sessions` WRITE;
/*!40000 ALTER TABLE `user_sessions` DISABLE KEYS */;
INSERT INTO `user_sessions` VALUES (1,'2026-08-24 06:41:44.158991','0:0:0:0:0:0:0:1','c7e772fe-b14f-4506-ba8f-0022403d3426','2026-08-24 06:41:44.158991','Mozilla/5.0 (Windows NT 10.0) Chrome/120.0 Safari/537.36',7),(2,'2026-08-24 06:41:44.853414','0:0:0:0:0:0:0:1','1bcaa112-d432-46b3-8655-99857ea2006f','2026-08-24 06:41:44.853414','Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Firefox/120.0',7),(3,'2026-08-24 06:41:45.411018','0:0:0:0:0:0:0:1','deb732ec-1b36-4772-a919-3de760d85ce1','2026-08-24 06:41:45.411018','Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Safari/604.1',7),(7,'2026-08-24 06:43:37.832101','0:0:0:0:0:0:0:1','bb589a9c-dd8c-43e5-9f0a-36cc8e4ea79b','2026-08-24 06:43:37.832101','Mozilla/5.0 (Windows NT 10.0) Edg/120.0',8),(29,'2026-08-27 12:10:48.195848','0:0:0:0:0:0:0:1','6233d34c-07a7-4518-bf89-a039bf0d792d','2026-08-27 12:10:48.195848','curl/8.19.0',14),(31,'2026-08-27 12:31:21.886477','0:0:0:0:0:0:0:1','1fc73cfe-c88d-492e-8fa3-24b8a1ea50da','2026-08-27 13:36:16.546287','curl/8.19.0',15),(37,'2026-09-04 07:07:42.134621','0:0:0:0:0:0:0:1','a21a195a-4ce7-4fa8-802d-f7c570b17792','2026-09-04 07:10:18.785962','Mozilla/5.0 (Windows NT; Windows NT 10.0; en-IN) WindowsPowerShell/5.1.26100.9168',17),(38,'2026-09-04 07:07:54.253841','0:0:0:0:0:0:0:1','46aa2669-2a9e-4d99-a77d-9412e2b01e56','2026-09-04 07:07:54.253841','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Claude/1.44121.4 Chrome/148.0.7778.280 Safari/537.36 MSIX',17),(44,'2026-09-12 12:17:03.032680','192.168.29.23','8f3302d4-a23b-4393-8b2f-3511b72315d9','2026-09-19 07:38:12.830018','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36',1),(51,'2026-09-19 06:22:36.596959','0:0:0:0:0:0:0:1','7199154a-923c-4da6-bf8b-13f63cab6ee0','2026-09-19 06:22:36.596959','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36',1),(90,'2026-10-06 11:50:30.091780','0:0:0:0:0:0:0:1','7226febf-0df2-41a7-9f32-ddfa1cfeb0aa','2026-10-06 13:54:35.940600','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36',1),(91,'2026-10-06 13:13:21.006751','0:0:0:0:0:0:0:1','761a1c2a-2a90-4c24-8a7a-ffef63dd37a2','2026-10-06 13:13:21.006751','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Claude/2.19675.1 Chrome/152.0.7977.130 Safari/537.36 MSIX',20);
/*!40000 ALTER TABLE `user_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_types`
--

DROP TABLE IF EXISTS `user_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `display_order` int NOT NULL,
  `name` varchar(50) NOT NULL,
  `epm_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8e5n03eqtc9alk98s41o00u5v` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_types`
--

LOCK TABLES `user_types` WRITE;
/*!40000 ALTER TABLE `user_types` DISABLE KEYS */;
INSERT INTO `user_types` VALUES (1,_binary '','2026-09-30 11:28:16.329828',0,'Individual',1),(2,_binary '','2026-09-30 11:28:16.397211',1,'Institutional',0),(3,_binary '','2026-09-30 11:28:16.410743',2,'International Buyer',NULL),(4,_binary '','2026-09-30 11:28:16.424715',3,'Student',3),(5,_binary '','2026-09-30 11:28:16.434775',4,'Business Collaborator',2),(6,_binary '','2026-09-30 11:28:16.449337',5,'Guest',5),(7,_binary '','2026-09-30 11:28:16.462844',6,'Government',NULL),(8,_binary '','2026-09-30 11:28:16.479551',7,'Executive',4);
/*!40000 ALTER TABLE `user_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `district` varchar(255) NOT NULL,
  `dob` date NOT NULL,
  `education` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `gender` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `middle_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `user_type` varchar(255) NOT NULL,
  `profile_image_path` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKdu5v5sr43g5bfnji4vb8hg5s3` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'vijayawada','2026-08-12 09:50:22.717099','Eluru','1999-06-20','graduate','pavan.feed.dev@gmail.com','chikati','Male','kalyan','pavan','$2a$12$WrOGxaz01TrWpbBT4tZ2uOiUx8o7Fztj2TDG7YlEjzrVE.0QsqM2G','9398821180','Andhra Pradesh','Individual',NULL,'USER'),(2,'Hyderabad','2026-08-13 07:16:42.275774','Hyderabad','2000-01-01','Bachelors','testuser.diag@example.com','Test','Male','User','','$2a$12$RPIOkR1sHMSnd7kcudfdguoeMNafIHlYZAF4UqBW/4e2Gqdq32tVm','9876543210','Telangana','Individual',NULL,NULL),(3,'Ajith singh nagar, Dabba kottlu centre ,vijayawada, krishna dist, Andhra Pradesh -520015','2026-08-14 07:41:53.843813','Krishna','1998-04-17','Degree','rupa.reddy0417@gmail.com','Rupa','Female','Reddy','','$2a$12$Y3qXKvDtBiM3sgo9gIv3Wu7CHmxnnUBP6KHZbYo83p2rx2D9GDxSu','9346565132','Andhra Pradesh','Individual',NULL,NULL),(4,'vijayawada','2026-08-14 07:45:50.677895','NTR','1998-01-10','MBA','yashwanthkalyan911@gmail.com','YK','Male','v','V','$2a$12$qpmkrCg3.sr3osOoTX5Y/ulzzRZ7YG1XfDwgGUWLw0QwwUAps4wpi','8801689161','Andhra Pradesh','Individual',NULL,NULL),(5,'Hyderabad','2026-08-14 11:05:51.698960','Hyderabad','2000-01-01','Bachelors','testuser_navtest@example.com','Test','Male','User','','$2a$12$eaofSQIUA4PX.6hX5w7IBuTNftafL4aFY/AiObPiY7w5ZqEWUVtwy','9123456789','Telangana','Individual',NULL,NULL),(6,'Bengaluru','2026-08-22 12:58:35.478699','Bengaluru','1990-01-01','Graduate','ers.test.user@example.com','Test','male','ErsUser',NULL,'$2a$12$tBT1qemyjpeprR2NVG1o/u65nqVz8vnoMlsP1K2S.QUgqbUc29.pG','9999900001','Karnataka','farmer',NULL,NULL),(7,'Hyd','2026-08-24 06:41:34.377394','Hyd','1995-01-01','UG','session_test_1787553693@example.com','Session','male','Tester',NULL,'$2a$12$g9jAPcRZjxf1TII3onSGWOzmtIzkZIr/X/aNCT/rs11WhSuq3dqjq','9787553693','TS','individual',NULL,NULL),(8,'Hyd','2026-08-24 06:43:19.321209','Hyd','1995-01-01','UG','session_test_1787553798@example.com','Session','male','Tester',NULL,'$2a$12$l2EicJsbBUxBNbTElUHAxe3O.xmNx7fxA.e3NxHo8gSIA9l7F/1Xm','9787553798','TS','individual',NULL,NULL),(14,'Guntur','2026-08-27 12:10:41.098821','Guntur','1995-01-01','Graduate','claude.pdftest2@example.com','Claude','Others','Tester','','$2a$12$nJ9RXS5vtzDwm8JqdBhVXenmKyrw0leNvHFqv2fnPdBtYlbESNBKi','9123456780','Andhra Pradesh','Individual',NULL,NULL),(15,'Bengaluru','2026-08-27 12:31:21.227380','Bengaluru','1990-01-01','Graduate','claude.bugfix.test@example.com','Claude','Other','Tester',NULL,'$2a$12$74oH6B5SopSwq6hi9OtgrOQ9tPXJ8inOi.UR4PJEZmvGjy9vglmkK','9000011122','Karnataka','Individual',NULL,NULL),(17,'Hyderabad','2026-09-04 07:07:33.204505','Hyderabad','1995-05-20','Graduate','navtest@example.com','Krushiye','Male','Vajayate',NULL,'$2a$12$0WgAHgv8qrdcgdLLxHe6yuSSJ7tENnAzbCG15abgabB47nPngVJAq','9876543211','Telangana','Farmer',NULL,NULL),(19,'vijayawada','2026-09-18 13:13:34.722574','Krishna','1999-06-20','graduate','admin@feedworld.com','pavan','Male','ch','kalyan','$2a$12$4LCeS9fZ6UoARnAvxLeUEuSReaOqtAzt31/kgWG.I7nQedZSHZOha','9642427354','Andhra Pradesh','International Buyer',NULL,'USER'),(20,'Vijayawada','2026-10-02 05:55:06.287358','Krishna','1995-06-15','Graduate','testuser@feed.test','Test','Male','User',NULL,'$2a$12$L6wRBI7VuEjVMlzwDYTR3uJl/ixxlZ.3K4QoVERtAFG20gfzZ4rFS','9688289170','Andhra Pradesh','Individual',NULL,'USER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06 19:24:55
