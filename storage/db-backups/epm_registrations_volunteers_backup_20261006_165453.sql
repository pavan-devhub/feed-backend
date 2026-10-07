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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epm_registrations`
--

LOCK TABLES `epm_registrations` WRITE;
/*!40000 ALTER TABLE `epm_registrations` DISABLE KEYS */;
INSERT INTO `epm_registrations` VALUES (2,_binary '','2026-09-04 06:31:26.121762','Krishna',NULL,60,'Nagpur','2026-09-06','Maharashtra','mohan pavan kalyan','9398821180','Exporter','andhra pradesh',1,5),(3,_binary '','2026-09-04 06:46:32.729646','krishna',NULL,60,'Nagpur','2026-09-06','Maharashtra','devadatta','9642427354','Entrepreneur','Andhra Pradesh',19,5),(4,_binary '','2026-09-04 07:14:04.253712','Krishna',NULL,76,'Bhopal','2026-09-08','Madhya Pradesh','anand','6302129460','Exporter','Andhra Pradesh',NULL,5),(5,_binary '','2026-09-04 07:58:23.306465','Krishna',NULL,91,'Indore','2026-09-13','Madhya Pradesh','sravya','5478523585','Entrepreneur','Andhra Pradesh',NULL,5),(6,_binary '','2026-09-04 09:31:22.637835','Nashik',NULL,60,'Nagpur','2026-09-06','Maharashtra','Test Registrant','9876543210','Farmer','Maharashtra',2,1),(7,_binary '','2026-09-04 10:17:15.965811','Nashik',NULL,60,'Nagpur','2026-09-06','Maharashtra','Dynamic Test Reg','9123456780','Farmer','Maharashtra',14,1),(8,_binary '','2026-09-19 10:33:37.736528','Krishna','mohanpavankalyan53@gmail.com',26,'Kolkata','2026-09-19','West Bengal','Mohan pavan kalyan','9398821180','FPO','Andhra Pradesh',1,2),(9,_binary '','2026-10-02 06:55:59.466062','hyderabad',NULL,50,'Ahmedabad','2026-10-03','Gujarat','anand','6302129460','FPO','telangana',19,2),(10,_binary '\0','2026-10-06 06:37:08.698885','Eluru','pavan.feed.dev@gmail.com',34,'Madurai','2026-10-07','Tamil Nadu','chikati pavan kalyan','9398821180',NULL,'Andhra Pradesh',1,1);
/*!40000 ALTER TABLE `epm_registrations` ENABLE KEYS */;
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
INSERT INTO `epm_volunteers` VALUES (2,'2026-09-04 09:30:29.692593','Nashik',NULL,60,'Nagpur','2026-09-06','Maharashtra','Student','Test Volunteer','9876543210',NULL,'Maharashtra',2,4),(3,'2026-09-04 10:17:23.295738','Nashik',NULL,60,'Nagpur','2026-09-06','Maharashtra','Student','Dynamic Test Vol','9123456781',NULL,'Maharashtra',NULL,4);
/*!40000 ALTER TABLE `epm_volunteers` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06 16:54:53
