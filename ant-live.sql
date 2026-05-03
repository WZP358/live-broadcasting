-- MySQL dump 10.13  Distrib 8.4.7, for Win64 (x86_64)
--
-- Host: localhost    Database: ant-live
-- ------------------------------------------------------
-- Server version	8.4.7

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
-- Table structure for table `auth`
--

DROP TABLE IF EXISTS `auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `positive_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `reverse_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `card_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `status` int NOT NULL,
  `hand_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `operator` int DEFAULT NULL,
  `reject_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth`
--

LOCK TABLES `auth` WRITE;
/*!40000 ALTER TABLE `auth` DISABLE KEYS */;
INSERT INTO `auth` VALUES (9,10001,'番茄蛋','http://image.imhtb.cn/avatar.png','http://image.imhtb.cn/avatar.png','777888333378777727',1,'http://image.imhtb.cn/avatar.png','2020-05-20 15:50:13','2020-05-22 13:37:29',0,NULL);
/*!40000 ALTER TABLE `auth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ban_record`
--

DROP TABLE IF EXISTS `ban_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ban_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int DEFAULT NULL,
  `resume_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `status` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ban_record`
--

LOCK TABLES `ban_record` WRITE;
/*!40000 ALTER TABLE `ban_record` DISABLE KEYS */;
INSERT INTO `ban_record` VALUES (1,1001,'2020-05-23 18:46:15','2020-05-23 18:46:17','2020-05-23 18:46:19','封禁原因','2020-05-23 18:46:26','备注',0),(2,16,'2020-05-27 16:00:00','2020-05-27 20:54:14',NULL,'涉黄','2020-05-27 20:54:14','手动恢复',1);
/*!40000 ALTER TABLE `ban_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bill`
--

DROP TABLE IF EXISTS `bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `bill_change` decimal(10,2) NOT NULL,
  `type` int NOT NULL,
  `balance` decimal(10,2) NOT NULL,
  `ip` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `order_no` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK_BILL_USER_ID` (`user_id`) USING BTREE,
  CONSTRAINT `FK_BILL_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=84 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bill`
--

LOCK TABLES `bill` WRITE;
/*!40000 ALTER TABLE `bill` DISABLE KEYS */;
INSERT INTO `bill` VALUES (1,10001,200.00,0,200.00,'1',NULL,'2020-03-16 11:15:50',NULL,'587946745648'),(2,10001,-2.00,1,198.00,'1',NULL,'2020-03-11 11:15:52',NULL,'323934545648'),(3,10002,0.00,0,0.00,'1',NULL,'2020-03-25 01:52:17',NULL,'587946745648'),(6,10001,88.88,1,109.12,NULL,'向直播间2赠送礼物','2020-03-25 01:54:15',NULL,'587946745648'),(7,10002,88.88,0,88.88,NULL,'收获礼物','2020-03-25 01:54:15',NULL,'587946745648'),(8,10001,99.99,1,9.13,NULL,'向直播间2赠送礼物','2020-03-25 01:54:23',NULL,'587946745648'),(9,10002,99.99,0,188.87,NULL,'收获礼物','2020-03-25 01:54:23',NULL,'587946745648'),(10,10001,100000.00,0,100000.00,NULL,NULL,'2020-04-06 19:54:09',NULL,'587946745648'),(11,10001,88000.00,1,12000.00,NULL,'赠送礼物','2020-04-06 19:54:37',NULL,NULL),(12,10002,88000.00,0,88188.87,NULL,'收获礼物','2020-04-06 19:54:37',NULL,NULL),(13,10001,1000.00,1,11000.00,NULL,'赠送礼物','2020-04-06 19:55:04',NULL,NULL),(14,10002,1000.00,0,89188.87,NULL,'收获礼物','2020-04-06 19:55:04',NULL,NULL),(15,10001,-100.00,1,10900.00,NULL,'提现','2020-04-08 18:34:42',NULL,'123118471236123'),(16,10001,-1000.00,1,9900.00,NULL,'提现','2020-04-08 19:12:52',NULL,'123118471236123'),(17,10001,-1000.00,1,8900.00,NULL,'提现','2020-04-08 19:13:36',NULL,'123118471236123'),(18,10001,-1000.00,1,7900.00,NULL,'提现','2020-04-08 19:15:08',NULL,'123118471236123'),(19,10001,-1000.00,1,6900.00,NULL,'提现','2020-04-08 19:16:49',NULL,'123118471236123'),(20,10001,-1000.00,1,5900.00,NULL,'提现','2020-04-08 19:27:05',NULL,'123118471236123'),(21,10001,-1000.00,1,4900.00,NULL,'提现','2020-04-08 19:30:10',NULL,'1a40b7bceb8270e7d96a94'),(22,10001,-1000.00,1,3900.00,NULL,'提现','2020-04-08 19:31:02',NULL,'5742b0ae83672c63b992ab'),(23,10001,-1000.00,1,2900.00,NULL,'提现','2020-04-08 19:31:19',NULL,'274a41bc9d4adf6aa4188f'),(24,10001,-1000.00,1,1900.00,NULL,'提现','2020-04-08 19:31:25',NULL,'2540099e4277d072e2a816'),(25,10001,-1000.00,1,900.00,NULL,'提现','2020-04-08 19:31:28',NULL,'044cdabc0de3367d6675f3'),(26,10001,-100.00,1,800.00,NULL,'提现','2020-04-09 23:20:24',NULL,'9644fd801615909ecccb0c'),(27,10001,-100.00,1,700.00,NULL,'提现','2020-04-09 23:30:46',NULL,'014dada040cef8aebe4292'),(28,10001,-100.00,1,600.00,NULL,'提现','2020-04-09 23:39:13',NULL,'414c7b937cc84bde6042a2'),(29,10001,-100.00,1,500.00,NULL,'提现','2020-04-10 00:12:12',NULL,'7d4be2a2e4aacdaed5c331'),(30,10001,-100.00,1,400.00,NULL,'提现','2020-04-10 00:15:09',NULL,'e647c0947baa3f69cb017e'),(31,10001,-100.00,1,300.00,NULL,'提现','2020-04-10 00:20:50',NULL,'694e7b96bd2528308c8f18'),(32,10001,-10.00,1,290.00,NULL,'提现','2020-04-11 12:13:43',NULL,'944608a5d6cc68aeefddf1'),(35,10001,-10.00,1,280.00,NULL,'赠送礼物','2020-04-11 13:18:32',NULL,NULL),(36,10001,10.00,0,300.00,NULL,'收获礼物','2020-04-11 13:18:32',NULL,NULL),(37,10001,-10.00,1,290.00,NULL,'赠送礼物','2020-04-11 13:19:11',NULL,NULL),(38,10001,10.00,0,310.00,NULL,'收获礼物','2020-04-11 13:19:11',NULL,NULL),(39,10001,-10.00,1,300.00,NULL,'赠送礼物','2020-04-11 13:19:30',NULL,NULL),(40,10001,10.00,0,320.00,NULL,'收获礼物','2020-04-11 13:19:30',NULL,NULL),(41,10001,-10.00,1,310.00,NULL,'赠送礼物','2020-04-11 13:20:24',NULL,NULL),(42,10001,10.00,0,330.00,NULL,'收获礼物','2020-04-11 13:20:24',NULL,NULL),(43,10001,-10.00,1,320.00,NULL,'赠送礼物','2020-04-11 13:20:31',NULL,NULL),(44,10001,10.00,0,340.00,NULL,'收获礼物','2020-04-11 13:20:31',NULL,NULL),(45,10001,-10.00,1,330.00,NULL,'赠送礼物','2020-04-11 13:23:04',NULL,NULL),(46,10001,10.00,0,350.00,NULL,'收获礼物','2020-04-11 13:23:04',NULL,NULL),(47,10007,0.00,0,0.00,NULL,'初始化账单','2020-05-09 11:58:29','2020-05-09 11:58:29',NULL),(48,10001,640.00,0,990.00,NULL,NULL,'2020-05-11 23:53:30','2020-05-11 23:53:30','724847b0459cbc32954b0b'),(49,10001,-90.00,1,900.00,NULL,'提现','2020-05-11 23:54:40','2020-05-11 23:54:40','564032b50b2b30149a46d7'),(50,10001,-100.00,1,800.00,NULL,'提现','2020-05-12 00:01:40','2020-05-12 00:01:40','61459ea8206ac4aef665f1'),(51,10001,640.00,0,1440.00,NULL,NULL,'2020-05-12 00:23:28','2020-05-12 00:23:28','0843b2bfd8cf1d10b113a7'),(52,10001,-440.00,1,1000.00,NULL,'提现','2020-05-12 00:24:03','2020-05-12 00:24:03','664649aab9ba0c5e35a7f1'),(53,10008,0.00,0,0.00,NULL,'初始化账单','2020-05-13 17:34:19','2020-05-13 17:34:19',NULL),(54,10009,0.00,0,0.00,NULL,'初始化账单','2020-05-18 12:52:48','2020-05-18 12:52:48',NULL),(55,10001,640.00,0,1640.00,NULL,NULL,'2020-05-20 16:06:28','2020-05-20 16:06:28','0f4a908469d9b1807b5084'),(56,10001,-300.00,1,1340.00,NULL,'提现','2020-05-20 16:07:35','2020-05-20 16:07:35','ef4b5092e41722a0da8805'),(63,10001,-10.00,1,1330.00,NULL,'赠送礼物','2020-05-25 22:47:13','2020-05-25 22:47:13',NULL),(64,10002,10.00,0,89198.87,NULL,'收获礼物','2020-05-25 22:47:13','2020-05-25 22:47:13',NULL),(65,10002,-1000.00,0,88198.87,NULL,'视频打赏','2020-05-26 11:26:58','2020-05-26 11:26:58','a14142a0602b08ca2613e5'),(66,10001,1000.00,0,2330.00,NULL,'视频打赏','2020-05-26 11:26:58','2020-05-26 11:26:58','734696a97e485dde8999df'),(67,10001,-100.00,0,2230.00,NULL,'直播打赏','2020-05-26 11:29:53','2020-05-26 11:29:53','8f410f8f91f7df66297dc6'),(68,10002,100.00,0,88298.87,NULL,'直播打赏','2020-05-26 11:29:53','2020-05-26 11:29:53','5348a6b0618dc676c07d5d'),(69,10001,-100.00,0,2130.00,NULL,'直播打赏','2020-05-26 11:30:09','2020-05-26 11:30:09','304b5ba33afa93c44b8d20'),(70,10002,100.00,0,88398.87,NULL,'直播打赏','2020-05-26 11:30:09','2020-05-26 11:30:09','274d5cb27bbb251f941688'),(71,10001,-1000.00,0,1130.00,NULL,'直播打赏','2020-05-26 11:30:43','2020-05-26 11:30:43','b64681b2df4c2dc93737e8'),(72,10002,1000.00,0,89398.87,NULL,'直播打赏','2020-05-26 11:30:43','2020-05-26 11:30:43','2142ae8f0a9ed430c1d8d2'),(73,10010,0.00,0,0.00,NULL,'初始化账单','2020-05-27 16:58:14','2020-05-27 16:58:14',NULL),(74,10011,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:01:37','2020-05-27 17:01:37',NULL),(75,10012,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:03:04','2020-05-27 17:03:04',NULL),(76,10013,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:04:30','2020-05-27 17:04:30',NULL),(77,10014,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:06:09','2020-05-27 17:06:09',NULL),(78,10015,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:08:06','2020-05-27 17:08:06',NULL),(79,10016,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:10:31','2020-05-27 17:10:31',NULL),(80,10017,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:14:41','2020-05-27 17:14:41',NULL),(81,10018,0.00,0,0.00,NULL,'初始化账单','2020-05-27 17:57:45','2020-05-27 17:57:45',NULL),(82,10001,-10.00,1,1120.00,NULL,'视频打赏','2021-11-23 23:29:42','2021-11-23 23:29:42','e5452c963e31822fb6fd0e'),(83,10001,10.00,0,1140.00,NULL,'视频打赏','2021-11-23 23:29:42','2021-11-23 23:29:42','204d4c9fd63d4cb0ac52cf');
/*!40000 ALTER TABLE `bill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `disabled` int DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `parent_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'游戏直播',1,0,0,'2020-04-19 01:33:15','2020-04-19 01:33:17',NULL),(2,'娱乐直播',1,0,0,'2020-04-19 01:33:35','2020-04-19 01:33:37',NULL);
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `live_detect`
--

DROP TABLE IF EXISTS `live_detect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `live_detect` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int DEFAULT NULL,
  `type` int DEFAULT NULL,
  `confidence` int DEFAULT NULL,
  `img` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `normal_score` int DEFAULT NULL,
  `hot_score` int DEFAULT NULL,
  `porn_score` int DEFAULT NULL,
  `level` int DEFAULT NULL,
  `polity_score` int DEFAULT NULL,
  `illegal_score` int DEFAULT NULL,
  `terror_score` int DEFAULT NULL,
  `handle_status` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `screenshot_time` int DEFAULT NULL,
  `resume_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `live_detect`
--

LOCK TABLES `live_detect` WRITE;
/*!40000 ALTER TABLE `live_detect` DISABLE KEYS */;
INSERT INTO `live_detect` VALUES (1,1,1,99,'http://image.imhtb.cn/avatar.png',1,0,99,0,0,0,0,1,'2020-05-13 18:17:26',NULL,1589365045,'2020-05-14 02:17:26'),(2,3,1,99,'http://image.imhtb.cn/avatar.png',1,0,99,0,0,0,0,1,'2020-05-13 18:23:36',NULL,1589365415,'2020-05-14 02:23:36');
/*!40000 ALTER TABLE `live_detect` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `live_info`
--

DROP TABLE IF EXISTS `live_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `live_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `room_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `status` int DEFAULT NULL,
  `click_count` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `dan_mu_count` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `present_count` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK_ROOM_INFO_ROOM_ID` (`room_id`) USING BTREE,
  CONSTRAINT `FK_ROOM_INFO_ROOM_ID` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=67 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `live_info`
--

LOCK TABLES `live_info` WRITE;
/*!40000 ALTER TABLE `live_info` DISABLE KEYS */;
INSERT INTO `live_info` VALUES (8,'2020-03-05 18:00:30','2020-03-05 18:04:41',1,10001,'2020-03-05 18:00:30','2020-03-05 18:04:41',1,NULL,NULL,NULL),(9,'2020-03-05 18:05:37','2020-03-05 18:14:08',1,10001,'2020-03-05 18:05:37','2020-03-05 18:14:08',1,NULL,NULL,NULL),(10,'2020-03-18 16:48:33','2020-03-18 16:49:29',1,10001,'2020-03-18 16:48:33','2020-03-18 16:49:29',1,NULL,NULL,NULL),(11,'2020-03-18 16:50:57','2020-03-18 16:51:18',1,10001,'2020-03-18 16:50:57','2020-03-18 16:51:18',1,NULL,NULL,NULL),(12,'2020-03-18 17:43:38','2020-03-18 17:53:17',1,10001,'2020-03-18 17:43:38','2020-03-18 17:53:17',1,NULL,NULL,NULL),(13,'2020-05-10 23:52:49','2020-05-10 23:54:41',1,10001,'2020-05-10 23:52:49','2020-05-10 23:54:41',1,NULL,NULL,NULL),(14,'2020-05-10 23:55:33','2020-05-10 23:58:12',1,10001,'2020-05-10 23:55:33','2020-05-10 23:58:12',1,NULL,NULL,NULL),(15,'2020-05-13 16:28:40','2020-05-13 16:32:12',1,10001,'2020-05-13 16:28:40','2020-05-13 16:32:12',1,NULL,NULL,NULL),(16,'2020-05-13 16:36:12','2020-05-13 16:38:19',1,10001,'2020-05-13 16:36:12','2020-05-13 16:38:19',1,NULL,NULL,NULL),(17,'2020-05-13 16:54:04','2020-05-13 17:02:34',1,10001,'2020-05-13 16:54:04','2020-05-13 17:02:34',1,NULL,NULL,NULL),(18,'2020-05-13 17:12:31','2020-05-13 17:16:51',1,10001,'2020-05-13 17:12:31','2020-05-13 17:16:51',1,NULL,NULL,NULL),(19,'2020-05-13 17:17:25','2020-05-13 17:19:32',1,10001,'2020-05-13 17:17:25','2020-05-13 17:19:32',1,NULL,NULL,NULL),(20,'2020-05-13 17:20:47','2020-05-13 17:22:32',1,10001,'2020-05-13 17:20:47','2020-05-13 17:22:32',1,NULL,NULL,NULL),(21,'2020-05-13 17:27:37','2020-05-13 17:30:14',1,10001,'2020-05-13 17:27:37','2020-05-13 17:30:14',1,NULL,NULL,NULL),(22,'2020-05-13 17:34:48','2020-05-13 17:37:32',1,10001,'2020-05-13 17:34:48','2020-05-13 17:37:32',1,NULL,NULL,NULL),(23,'2020-05-13 17:40:28','2020-05-13 17:45:28',1,10001,'2020-05-13 17:40:28','2020-05-13 17:45:28',1,NULL,NULL,NULL),(24,'2020-05-13 17:53:46','2020-05-13 17:54:20',1,10001,'2020-05-13 17:53:46','2020-05-13 17:54:20',1,NULL,NULL,NULL),(25,'2020-05-13 18:10:54','2020-05-13 18:11:36',1,10001,'2020-05-13 18:10:54','2020-05-13 18:11:36',1,NULL,NULL,NULL),(26,'2020-05-13 18:13:22','2020-05-13 18:13:59',1,10001,'2020-05-13 18:13:22','2020-05-13 18:13:59',1,NULL,NULL,NULL),(27,'2020-05-13 18:15:06','2020-05-13 18:15:15',1,10001,'2020-05-13 18:15:06','2020-05-13 18:15:15',1,NULL,NULL,NULL),(28,'2020-05-13 18:15:55','2020-05-13 18:16:02',1,10001,'2020-05-13 18:15:55','2020-05-13 18:16:02',1,NULL,NULL,NULL),(29,'2020-05-13 18:17:21','2020-05-13 18:17:51',1,10001,'2020-05-13 18:17:21','2020-05-13 18:17:51',1,NULL,NULL,NULL),(30,'2020-05-13 18:23:32','2020-05-13 18:23:43',1,10001,'2020-05-13 18:23:32','2020-05-13 18:23:43',1,NULL,NULL,NULL),(31,'2020-05-17 10:56:34','2020-05-17 10:59:52',1,10001,'2020-05-17 10:56:34','2020-05-17 10:59:52',1,NULL,NULL,NULL),(32,'2020-05-19 09:22:12','2020-05-19 09:23:01',1,10001,'2020-05-19 09:22:12','2020-05-19 09:23:01',1,NULL,NULL,NULL),(33,'2020-05-20 15:55:07','2020-05-20 15:57:59',1,10001,'2020-05-20 15:55:07','2020-05-20 15:57:59',1,NULL,NULL,NULL),(34,'2020-05-20 15:58:41','2020-05-20 15:59:36',1,10001,'2020-05-20 15:58:41','2020-05-20 15:59:36',1,NULL,NULL,NULL),(35,'2020-05-20 16:59:16','2020-05-20 16:59:30',1,10001,'2020-05-20 16:59:16','2020-05-20 16:59:30',1,NULL,NULL,NULL),(36,'2026-04-21 16:45:46',NULL,1,10001,'2026-04-21 16:45:46','2026-04-21 16:45:46',0,NULL,NULL,NULL),(37,'2026-04-21 16:45:48','2026-04-21 17:06:24',1,10001,'2026-04-21 16:45:48','2026-04-21 16:45:48',1,'1','0','0'),(38,'2026-04-21 17:06:28','2026-04-21 17:26:55',1,10001,'2026-04-21 17:06:28','2026-04-21 17:06:28',1,'2','0','0'),(39,'2026-04-21 17:27:02','2026-04-21 17:45:40',1,10001,'2026-04-21 17:27:02','2026-04-21 17:27:02',1,'1','0','0'),(40,'2026-04-21 17:46:17','2026-04-21 17:50:56',1,10001,'2026-04-21 17:46:17','2026-04-21 17:46:17',1,'1','0','0'),(41,'2026-04-21 17:51:00',NULL,1,10001,'2026-04-21 17:51:00','2026-04-21 17:51:00',0,NULL,NULL,NULL),(42,'2026-04-22 13:40:35','2026-04-22 13:46:50',17,10020,'2026-04-22 13:40:35','2026-04-22 13:40:35',1,'1','0','0'),(43,'2026-04-22 13:48:44','2026-04-22 13:49:53',17,10020,'2026-04-22 13:48:44','2026-04-22 13:48:44',1,'1','0','0'),(44,'2026-04-22 13:50:32','2026-04-22 13:51:45',17,10020,'2026-04-22 13:50:32','2026-04-22 13:50:32',1,'1','0','0'),(45,'2026-04-22 13:52:29','2026-04-22 13:54:10',17,10020,'2026-04-22 13:52:29','2026-04-22 13:52:29',1,'1','0','0'),(46,'2026-04-22 13:56:49','2026-04-22 14:09:38',17,10020,'2026-04-22 13:56:49','2026-04-22 13:56:49',1,'1','0','0'),(47,'2026-04-22 14:12:51','2026-04-22 14:15:38',17,10020,'2026-04-22 14:12:51','2026-04-22 14:12:51',1,'1','0','0'),(48,'2026-04-22 15:25:38','2026-04-22 15:30:01',17,10020,'2026-04-22 15:25:38','2026-04-22 15:25:38',1,'1','0','0'),(49,'2026-04-22 16:05:25','2026-04-22 16:05:36',17,10020,'2026-04-22 16:05:25','2026-04-22 16:05:25',1,'0','0','0'),(50,'2026-04-22 16:05:37','2026-04-22 16:05:59',17,10020,'2026-04-22 16:05:37','2026-04-22 16:05:37',1,'0','0','0'),(51,'2026-04-22 16:05:59','2026-04-22 17:18:16',17,10020,'2026-04-22 16:05:59','2026-04-22 16:05:59',1,'2','0','0'),(52,'2026-04-22 17:18:17','2026-04-22 17:23:27',17,10020,'2026-04-22 17:18:17','2026-04-22 17:18:17',1,'1','0','0'),(53,'2026-04-22 17:36:01','2026-04-22 18:03:11',17,10020,'2026-04-22 17:36:01','2026-04-22 17:36:01',1,'1','0','0'),(54,'2026-04-22 18:03:12','2026-04-22 18:04:19',17,10020,'2026-04-22 18:03:12','2026-04-22 18:03:12',1,'1','0','0'),(55,'2026-04-22 18:04:22','2026-05-03 15:08:51',17,10020,'2026-04-22 18:04:22','2026-04-22 18:04:22',1,NULL,NULL,NULL),(56,'2026-05-03 15:08:58','2026-05-03 15:09:09',17,10020,'2026-05-03 15:08:58','2026-05-03 15:08:58',1,NULL,NULL,NULL),(57,'2026-05-03 15:11:48','2026-05-03 15:12:49',17,10020,'2026-05-03 15:11:48','2026-05-03 15:11:48',1,NULL,NULL,NULL),(58,'2026-05-03 15:13:08','2026-05-03 15:14:11',18,10019,'2026-05-03 15:13:08','2026-05-03 15:13:08',1,NULL,NULL,NULL),(59,'2026-05-03 15:23:25','2026-05-03 15:27:18',18,10019,'2026-05-03 15:23:25','2026-05-03 15:23:25',1,NULL,NULL,NULL),(60,'2026-05-03 16:09:20','2026-05-03 16:28:44',18,10019,'2026-05-03 16:09:20','2026-05-03 16:09:20',1,NULL,NULL,NULL),(61,'2026-05-03 16:29:11','2026-05-03 16:29:11',18,10019,'2026-05-03 16:29:11','2026-05-03 16:29:11',1,NULL,NULL,NULL),(62,'2026-05-03 16:29:28','2026-05-03 16:29:36',18,10019,'2026-05-03 16:29:28','2026-05-03 16:29:28',1,NULL,NULL,NULL),(63,'2026-05-03 16:31:40','2026-05-03 16:31:47',18,10019,'2026-05-03 16:31:40','2026-05-03 16:31:40',1,NULL,NULL,NULL),(64,'2026-05-03 16:37:57','2026-05-03 16:38:05',18,10019,'2026-05-03 16:37:57','2026-05-03 16:37:57',1,NULL,NULL,NULL),(65,'2026-05-03 16:48:36','2026-05-03 16:54:39',18,10019,'2026-05-03 16:48:36','2026-05-03 16:48:36',1,NULL,NULL,NULL),(66,'2026-05-03 16:57:18','2026-05-03 16:58:43',18,10019,'2026-05-03 16:57:18','2026-05-03 16:57:18',1,NULL,NULL,NULL);
/*!40000 ALTER TABLE `live_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu`
--

DROP TABLE IF EXISTS `menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `menu_index` int DEFAULT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `pid` int DEFAULT '0',
  `sort` int DEFAULT NULL,
  `hidden` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu`
--

LOCK TABLES `menu` WRITE;
/*!40000 ALTER TABLE `menu` DISABLE KEYS */;
INSERT INTO `menu` VALUES (1,1,'el-icon-data-board','dashboard','首页',0,1,0,NULL,NULL),(2,2,'el-icon-user','user-manage','会员中心',0,2,0,NULL,NULL),(3,3,'el-icon-coordinate','user-auth','身份验证',0,3,0,NULL,NULL),(4,4,'el-icon-bangzhu','live-room-manage','直播管理',16,4,0,NULL,NULL),(5,5,'el-icon-data-analysis','live-info-manage','直播数据',16,5,0,NULL,NULL),(6,6,'el-icon-menu','system-settings','系统设置',0,6,1,NULL,NULL),(7,7,'el-icon-data-analysis','data-analysis','数据统计',0,90,0,NULL,'2020-05-23 19:41:03'),(8,8,'el-icon-goods','present-manage','礼物配置',0,8,0,NULL,'2020-05-27 11:18:51'),(9,9,'el-icon-s-shop','live-ban-manage','小黑屋',16,9,1,NULL,'2020-05-23 19:48:26'),(10,10,'el-icon-chat-line-round','message-push','消息推送',0,88,0,NULL,'2020-05-09 11:25:27'),(11,11,'el-icon-data-analysis','system-monitor-host','服务监控',12,11,0,NULL,NULL),(12,12,'el-icon-warning-outline','system-monitor','系统监控',0,18,0,NULL,'2020-05-27 11:18:18'),(13,13,'el-icon-data-analysis','system-manage','系统管理',0,13,0,NULL,NULL),(14,14,'el-icon-data-analysis','system-manage-menu','菜单管理',13,14,0,NULL,NULL),(15,15,'el-icon-data-analysis','system-manage-role','角色管理',13,15,0,NULL,NULL),(16,16,'el-icon-video-camera','live-center','直播中心',0,16,0,NULL,'2020-05-09 11:25:52'),(17,17,'el-icon-data-board','live-detect','截图检测',16,6,0,'2020-05-08 14:36:26','2020-05-23 19:47:54'),(18,18,'el-icon-data-analysis','bill','账单中心',0,4,0,'2020-05-09 11:11:18',NULL),(19,19,'el-icon-user','user-role-manage','角色分配',13,1,0,'2020-05-09 17:24:39','2020-05-19 11:39:04'),(20,20,'el-icon-data-board','snapshot-templates','鉴黄模板',16,1,0,'2020-05-22 14:32:58','2020-05-23 19:15:10'),(21,21,'el-icon-warning-outline','ban-record','封禁记录',16,1,0,'2020-05-23 18:27:47','2020-05-23 19:15:05'),(22,NULL,'el-icon-data-analysis','vod','视频中心',0,17,0,'2020-05-25 16:13:02','2020-05-27 11:17:53'),(23,NULL,'el-icon-data-analysis','video-manage','视频管理',22,1,0,'2020-05-25 16:13:51','2020-05-27 10:25:21'),(24,NULL,'el-icon-data-analysis','vod-list','稿件列表',22,2,0,'2020-05-27 11:51:19',NULL);
/*!40000 ALTER TABLE `menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `present`
--

DROP TABLE IF EXISTS `present`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `present` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `sort` int DEFAULT '0',
  `disabled` int DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `present`
--

LOCK TABLES `present` WRITE;
/*!40000 ALTER TABLE `present` DISABLE KEYS */;
INSERT INTO `present` VALUES (1,'火箭','http://image.imhtb.cn/飞机.png',10.00,'2020-02-26 18:20:48','2020-05-25 22:34:46',0,0),(2,'飞机','http://image.imhtb.cn/飞机1.png',88000.00,'2020-03-02 10:01:35','2020-05-25 22:34:39',3,0);
/*!40000 ALTER TABLE `present` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `present_reward`
--

DROP TABLE IF EXISTS `present_reward`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `present_reward` (
  `id` int NOT NULL AUTO_INCREMENT,
  `from_id` int DEFAULT NULL,
  `to_id` int DEFAULT NULL,
  `room_id` int DEFAULT NULL,
  `video_id` int DEFAULT NULL,
  `present_id` int DEFAULT NULL,
  `number` int DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `total_price` decimal(10,2) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `type` int DEFAULT NULL COMMENT '0-为直播间礼物 1-为视频打赏礼物',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK_ROOM_PRESENT_ROOM_ID` (`room_id`) USING BTREE,
  CONSTRAINT `FK_ROOM_PRESENT_ROOM_ID` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `present_reward`
--

LOCK TABLES `present_reward` WRITE;
/*!40000 ALTER TABLE `present_reward` DISABLE KEYS */;
INSERT INTO `present_reward` VALUES (5,10002,10001,2,NULL,2,1,88000.00,88000.00,'2020-04-06 19:54:37',NULL,NULL),(6,10002,10001,2,NULL,1,1,1000.00,1000.00,'2020-04-06 19:55:04',NULL,NULL),(8,10001,10001,1,NULL,1,1,10.00,10.00,'2020-04-11 13:18:32',NULL,NULL),(9,10001,10001,1,NULL,1,1,10.00,10.00,'2020-04-11 13:19:11',NULL,NULL),(10,10001,10001,1,NULL,1,1,10.00,10.00,'2020-04-11 13:19:30',NULL,NULL),(11,10001,10001,1,NULL,1,1,10.00,10.00,'2020-04-11 13:20:24',NULL,NULL),(12,10001,10001,1,NULL,1,1,10.00,10.00,'2020-04-11 13:20:31',NULL,NULL),(13,10001,10001,1,NULL,1,1,10.00,10.00,'2020-04-11 13:23:04',NULL,NULL),(17,10001,10002,2,NULL,1,1,10.00,10.00,'2020-05-25 22:47:13',NULL,1),(18,10002,10001,NULL,1,1,100,10.00,1000.00,'2020-05-26 11:26:58',NULL,1),(19,10001,10002,2,NULL,1,10,10.00,100.00,'2020-05-26 11:29:53',NULL,0),(20,10001,10002,2,NULL,1,10,10.00,100.00,'2020-05-26 11:30:09',NULL,0),(21,10001,10002,2,NULL,1,100,10.00,1000.00,'2020-05-26 11:30:43',NULL,0),(22,10001,10001,NULL,2,1,1,10.00,10.00,'2021-11-23 23:29:42',NULL,1);
/*!40000 ALTER TABLE `present_reward` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `level` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `permission` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'超级管理员',1,'2020-04-30 13:57:24','2020-04-30 13:57:27','ROLE_ROOT'),(2,'直播管理员',2,'2020-04-30 13:57:46','2020-04-30 13:57:48','ROLE_LIVE'),(100,'普通会员',8,'2020-04-30 15:16:56','2020-04-30 15:16:58','ROLE_COMMON'),(101,'身份认证管理员',2,'2020-05-09 11:21:03',NULL,'ROLE_AUTH');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_menu`
--

DROP TABLE IF EXISTS `role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_menu` (
  `role_id` int NOT NULL,
  `menu_id` int NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE,
  KEY `FK_ROLE_MENU_MENU_ID` (`menu_id`) USING BTREE,
  CONSTRAINT `FK_ROLE_MENU_MENU_ID` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_ROLE_MENU_ROLE_ID` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_menu`
--

LOCK TABLES `role_menu` WRITE;
/*!40000 ALTER TABLE `role_menu` DISABLE KEYS */;
INSERT INTO `role_menu` VALUES (1,1),(2,1),(101,1),(1,2),(1,3),(101,3),(1,4),(2,4),(1,5),(2,5),(1,7),(1,8),(1,9),(2,9),(1,10),(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(2,16),(1,17),(2,17),(1,18),(1,19),(1,20),(2,20),(1,21),(2,21),(1,22),(1,23),(1,24);
/*!40000 ALTER TABLE `role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room`
--

DROP TABLE IF EXISTS `room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `cover` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',
  `introduce` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `notice` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `user_id` int unsigned DEFAULT NULL,
  `rtmp_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `disabled` int DEFAULT '0',
  `status` int DEFAULT '-1',
  `category_id` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `secret` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `play_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK_ROOM_CATEGORY_ID` (`category_id`) USING BTREE,
  CONSTRAINT `FK_ROOM_CATEGORY_ID` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room`
--

LOCK TABLES `room` WRITE;
/*!40000 ALTER TABLE `room` DISABLE KEYS */;
INSERT INTO `room` VALUES (1,'','Spring Security','https://images.unsplash.com/photo-1582917205301-bbb4afb5501f?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=1350&q=80','简介','通知公告',10001,'http://play.imhtb.cn/live/',0,1,1,'2020-03-04 06:40:32','2026-04-21 17:06:37','1?lal_secret=5c1bf70154b2ed4c650573ff5be82c27',NULL),(2,'','官方直播间','https://images.unsplash.com/photo-1579599709180-375ce2a5db8b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=2125&q=80','这里是官方直播间','公告：24小时不间断直播',10002,'http://play.imhtb.cn/live/',0,1,1,'2020-03-02 10:36:16','2020-05-13 18:06:20','2',NULL),(5,NULL,'哈哈','https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/temp-15889987327273642532524691202410.jpg',NULL,'哈哈',10007,NULL,0,0,1,'2020-05-09 11:58:29','2020-05-13 18:06:19',NULL,NULL),(6,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10008,NULL,0,-1,NULL,'2020-05-13 17:34:19','2020-05-13 17:34:19',NULL,NULL),(7,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10009,NULL,0,0,NULL,'2020-05-18 12:52:48','2020-05-18 13:50:06',NULL,NULL),(8,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10010,NULL,0,-1,NULL,'2020-05-27 16:58:14','2020-05-27 16:58:14',NULL,NULL),(9,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10011,NULL,0,-1,NULL,'2020-05-27 17:01:37','2020-05-27 17:01:37',NULL,NULL),(10,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10012,NULL,0,-1,NULL,'2020-05-27 17:03:04','2020-05-27 17:03:04',NULL,NULL),(11,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10013,NULL,0,-1,NULL,'2020-05-27 17:04:30','2020-05-27 17:04:30',NULL,NULL),(12,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10014,NULL,0,-1,NULL,'2020-05-27 17:06:09','2020-05-27 17:06:09',NULL,NULL),(13,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10015,NULL,0,-1,NULL,'2020-05-27 17:08:06','2020-05-27 17:08:06',NULL,NULL),(14,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10016,NULL,0,-1,NULL,'2020-05-27 17:10:31','2020-05-27 17:10:31',NULL,NULL),(15,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10017,NULL,0,-1,NULL,'2020-05-27 17:14:41','2020-05-27 17:14:41',NULL,NULL),(16,NULL,NULL,'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',NULL,NULL,10018,NULL,0,-1,NULL,'2020-05-27 17:57:45','2020-05-27 17:57:45',NULL,NULL),(17,NULL,'帅哥1的直播间','https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg','这个主播还没有填写直播间简介','欢迎来到直播间',10020,NULL,0,0,NULL,'2026-04-22 13:40:28','2026-05-03 15:12:49','17?lal_secret=11be72210c7fe61f090cab26b6ae3072',NULL),(18,NULL,'帅哥的直播间','https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg','这个主播还没有填写直播间简介','欢迎来到直播间',10019,NULL,0,0,NULL,'2026-04-22 13:48:57','2026-05-03 16:58:44','18?lal_secret=10148bc6fbbfb02a05876485212d3fd3',NULL);
/*!40000 ALTER TABLE `room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_intimacy_rank`
--

DROP TABLE IF EXISTS `room_intimacy_rank`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_intimacy_rank` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL,
  `user_id` int NOT NULL,
  `intimacy_value` decimal(12,2) NOT NULL DEFAULT '0.00',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_user` (`room_id`,`user_id`),
  KEY `idx_room_intimacy_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播间亲密榜';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_intimacy_rank`
--

LOCK TABLES `room_intimacy_rank` WRITE;
/*!40000 ALTER TABLE `room_intimacy_rank` DISABLE KEYS */;
/*!40000 ALTER TABLE `room_intimacy_rank` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `statistic_speak`
--

DROP TABLE IF EXISTS `statistic_speak`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_speak` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `number` int DEFAULT NULL,
  `date` date DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `statistic_speak`
--

LOCK TABLES `statistic_speak` WRITE;
/*!40000 ALTER TABLE `statistic_speak` DISABLE KEYS */;
INSERT INTO `statistic_speak` VALUES (2,1,NULL,11,'2020-04-26','2020-04-27 00:01:02','2020-04-27 00:01:02'),(3,1,NULL,33,'2020-04-26','2020-04-27 00:07:00','2020-04-27 00:07:00'),(4,1,NULL,5,'2020-05-12','2020-05-13 08:00:05','2020-05-13 08:00:05'),(5,1,NULL,1,'2020-05-17','2020-05-18 08:00:05','2020-05-18 08:00:05'),(6,1,NULL,1,'2020-05-19','2020-05-20 08:00:06','2020-05-20 08:00:06'),(7,1,NULL,2,'2020-05-20','2020-05-21 08:00:05','2020-05-21 08:00:05');
/*!40000 ALTER TABLE `statistic_speak` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `statistic_view`
--

DROP TABLE IF EXISTS `statistic_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistic_view` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `member_number` int DEFAULT NULL,
  `visitor_number` int DEFAULT NULL COMMENT '游客浏览数',
  `total_number` int DEFAULT NULL,
  `date` date DEFAULT NULL,
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `statistic_view`
--

LOCK TABLES `statistic_view` WRITE;
/*!40000 ALTER TABLE `statistic_view` DISABLE KEYS */;
INSERT INTO `statistic_view` VALUES (1,1,1,123,23,146,'2020-04-12','','2020-04-12 13:31:27',NULL),(2,1,1,23,23,46,'2020-04-08','','2020-04-12 13:31:27',NULL),(3,1,NULL,0,5,5,'2020-04-26',NULL,'2020-04-27 00:07:00','2020-04-27 00:07:00'),(4,2,NULL,0,4,4,'2020-05-05',NULL,'2020-05-06 00:00:08','2020-05-06 00:00:08'),(5,2,NULL,0,5,5,'2020-05-09',NULL,'2020-05-10 08:00:05','2020-05-10 08:00:05'),(6,1,NULL,0,8,5,'2020-05-10',NULL,'2020-05-11 08:00:05','2020-05-11 08:00:05'),(7,1,NULL,0,3,3,'2020-05-11',NULL,'2020-05-12 08:00:05','2020-05-12 08:00:05'),(8,2,NULL,0,1,1,'2020-05-11',NULL,'2020-05-12 08:00:05','2020-05-12 08:00:05'),(9,2,NULL,0,1,1,'2020-05-12',NULL,'2020-05-13 08:00:05','2020-05-13 08:00:05'),(10,1,NULL,0,2,2,'2020-05-12',NULL,'2020-05-13 08:00:05','2020-05-13 08:00:05'),(11,1,NULL,0,3,3,'2020-05-13',NULL,'2020-05-14 08:00:05','2020-05-14 08:00:05'),(12,1,NULL,0,1,1,'2020-05-17',NULL,'2020-05-18 08:00:05','2020-05-18 08:00:05'),(13,1,NULL,0,1,1,'2020-05-18',NULL,'2020-05-19 08:00:05','2020-05-19 08:00:05'),(14,1,NULL,0,1,1,'2020-05-19',NULL,'2020-05-20 08:00:06','2020-05-20 08:00:06'),(15,1,NULL,0,3,3,'2020-05-20',NULL,'2020-05-21 08:00:05','2020-05-21 08:00:05'),(16,2,NULL,0,7,7,'2020-05-21',NULL,'2020-05-22 08:00:05','2020-05-22 08:00:05'),(17,2,NULL,0,1,1,'2020-05-22',NULL,'2020-05-23 08:00:05','2020-05-23 08:00:05');
/*!40000 ALTER TABLE `statistic_view` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_push`
--

DROP TABLE IF EXISTS `sys_push`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_push` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `open` int DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `listener_items` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FK_SYSTEM_PUSH_USER_ID` (`user_id`) USING BTREE,
  CONSTRAINT `FK_SYSTEM_PUSH_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_push`
--

LOCK TABLES `sys_push` WRITE;
/*!40000 ALTER TABLE `sys_push` DISABLE KEYS */;
INSERT INTO `sys_push` VALUES (2,'794409767@qq.com',NULL,10001,1,'2020-05-08 18:02:13','2020-05-27 18:13:00','salacity-notice');
/*!40000 ALTER TABLE `sys_push` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_push_log`
--

DROP TABLE IF EXISTS `sys_push_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_push_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `status` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `sys_push_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_push_log`
--

LOCK TABLES `sys_push_log` WRITE;
/*!40000 ALTER TABLE `sys_push_log` DISABLE KEYS */;
INSERT INTO `sys_push_log` VALUES (1,'房间ID：10001 色情检测置信度99',1,'2020-05-15 16:14:44','2020-05-20 16:14:42',2);
/*!40000 ALTER TABLE `sys_push_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_wallet`
--

DROP TABLE IF EXISTS `tb_wallet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_wallet` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `balance` decimal(16,2) NOT NULL DEFAULT '0.00',
  `version` int NOT NULL DEFAULT '0',
  `sign` varchar(255) DEFAULT NULL,
  `status` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tb_wallet_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_wallet`
--

LOCK TABLES `tb_wallet` WRITE;
/*!40000 ALTER TABLE `tb_wallet` DISABLE KEYS */;
INSERT INTO `tb_wallet` VALUES (1,10019,0.00,0,NULL,0,'2026-05-03 15:12:12','2026-05-03 15:12:12'),(2,10020,0.00,0,NULL,0,'2026-05-03 15:13:28','2026-05-03 15:13:28'),(3,10001,0.00,0,NULL,0,'2026-05-03 15:24:25','2026-05-03 15:24:25');
/*!40000 ALTER TABLE `tb_wallet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_wallet_log`
--

DROP TABLE IF EXISTS `tb_wallet_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_wallet_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `wallet_id` int NOT NULL,
  `balance` decimal(16,2) NOT NULL,
  `fee` decimal(16,2) NOT NULL,
  `action_type` int NOT NULL,
  `source_uuid` varchar(64) DEFAULT NULL,
  `source_type` varchar(32) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tb_wallet_log_source_uuid` (`source_uuid`),
  KEY `idx_tb_wallet_log_wallet_id` (`wallet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_wallet_log`
--

LOCK TABLES `tb_wallet_log` WRITE;
/*!40000 ALTER TABLE `tb_wallet_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_wallet_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `signature` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `birthday` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `sex` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `nick_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `is_validated` int DEFAULT '0',
  `disabled` int DEFAULT '0',
  `role_id` int NOT NULL DEFAULT '100',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10021 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (10001,'admin','$2a$10$puULYxVheVu/sJZk7rUbvujNheV9v7afPWETHv47sjS2KAXNptTEe',NULL,'http://image.imhtb.cn/avatar.png','个性签名',NULL,'男','PinTeh','2020-05-09 18:47:23','2020-05-09 11:33:16','794409767@qq.com',0,0,1),(10002,'10002','$2a$10$GU9Ya.QrkZu.0TNxO4BuG.B9x26pD7Yl8jQUTENz3OuB3mBTqlWeC',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100','个性签名',NULL,'女','官方直播账号','2020-05-09 18:47:25','2020-04-30 23:20:45','3633530052@qq.com',0,0,0),(10007,'10007','$2a$10$gZpGIsxQFU5tNCbLGr0uXuF3f7MYt2hKm66Mo532nblS.e6PX1KL.',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'Lequal','2020-05-09 11:58:29','2020-05-12 23:23:43','2818028189@qq.com',0,0,100),(10008,'10008','$2a$10$VWEIsoOddZB4JVrVPMOnT.Kh29X1FYWl5TvRqIGXjyyWSNoJhwHPK',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'lequal','2020-05-13 17:34:19','2020-05-13 18:00:40','1576070851@qq.com',0,0,100),(10009,'10009','$2a$10$M9M/hvCB42gZHqvbc2tzHOUZt1gVr0O5ZSsWyR/x4UpmzwsPxEbI.',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'测试用户1','2020-05-18 12:52:48','2020-05-19 11:43:40','456456456@qq.com',0,0,100),(10010,'10010','$2a$10$ihwTAs8./uFG2prd7SDenOU1jhUe2N.2.VDP/SpgkyF33XUEeezQG',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 16:58:14','2020-05-27 16:58:14','36335300253@qq.com',0,0,100),(10011,'10011','$2a$10$sFn2iW4/L9fcGMKl.eqgM.tC1GcX.jRiKvOO4rFEAlAg4fqD50aNC',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 17:01:37','2020-05-27 17:01:37','3633532005@qq.com',0,0,100),(10012,'10012','$2a$10$uj9RWwancllT112ht0Uuo.q1Y/F2HpC52yVZVuU35Jh781GzvUh1G',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 17:03:04','2020-05-27 17:03:04','36332523005@qq.com',0,0,100),(10013,'10013','$2a$10$ETUeMp76vPNy9.I/.nUtiOAM5XuS8753VUvy1V723TEr2CsNOAxCS',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 17:04:30','2020-05-27 17:04:30','36335230055@qq.com',0,0,100),(10014,'10014','$2a$10$tiX1FP1Dd6CxlTYihhcrG.wxU5Mn2CtFMEMwEaMiFwsYSpqlnOEc2',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 17:06:09','2020-05-27 17:06:09','36366353005@qq.com',0,0,100),(10015,'10015','$2a$10$rVdkt24Vyu/DNfbnYrlwEOjffgThjqv4LIL0wNB.xelDm.BgbMkAa',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 17:08:06','2020-05-27 17:08:06','36343524345@qq.com',0,0,100),(10016,'10016','$2a$10$cAInYzageMiHtw4lHnIR8ejwhAa4/vRODPCot5kZ127Uk880qODiS',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'123123','2020-05-27 17:10:31','2020-05-27 17:10:31','363355563005@qq.com',0,0,100),(10017,'10017','$2a$10$x3YS39ADdjwvEpbz6VwJQOn0SCCv3FTzvZPBjVRl8gQgzkRye90wG',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'12312','2020-05-27 17:14:41','2020-05-27 17:14:41','363353005@qq.com',0,0,100),(10018,'10018','$2a$10$nYPILKoi2sI8ixY1A5a/KOQeCl4K6ggJ1yxGeprinoVRIBkw/FUFK',NULL,'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100',NULL,NULL,NULL,'没有负担','2020-05-27 17:57:45','2020-05-27 17:58:09','123123123@qq.com',0,0,100),(10019,'root','$2a$10$9czLe1MBpr8DroePhrGYVeDtsVHMpC4j8oTiXZRSx6cfo8fWy8.0G',NULL,NULL,NULL,NULL,NULL,'帅哥','2026-04-21 16:45:07','2026-04-21 16:45:07',NULL,0,0,100),(10020,'root1','$2a$10$oZsxQvQosUg9gBqcawChIOvYJdhA3Dk6Ia/nOTRM6kfFURqVRq0Z.',NULL,NULL,NULL,NULL,NULL,'帅哥1','2026-04-22 12:41:00','2026-04-22 12:41:00',NULL,0,0,100);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE,
  KEY `FK_USER_ROLE_ROLE_ID` (`role_id`) USING BTREE,
  CONSTRAINT `FK_USER_ROLE_ROLE_ID` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_USER_ROLE_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role` DISABLE KEYS */;
INSERT INTO `user_role` VALUES (10001,1,NULL,NULL),(10002,2,NULL,NULL);
/*!40000 ALTER TABLE `user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video`
--

DROP TABLE IF EXISTS `video`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video` (
  `id` int NOT NULL AUTO_INCREMENT,
  `video_url` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `cover_url` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `file_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `video_category_id` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `status` int DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video`
--

LOCK TABLES `video` WRITE;
/*!40000 ALTER TABLE `video` DISABLE KEYS */;
INSERT INTO `video` VALUES (1,'http://1253825991.vod2.myqcloud.com/a62a57cfvodcq1253825991/d444c50c5285890802997421440/uCtVi1NBRxUA.mp4','https://images.unsplash.com/photo-1579599709180-375ce2a5db8b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=2125&q=80','5285890802997421440',NULL,10001,NULL,'2020-05-25 15:00:29','2020-05-27 18:18:16','Spring Redis Startar',0),(2,'http://1253825991.vod2.myqcloud.com/a62a57cfvodcq1253825991/8bd2d61b5285890803425878758/z1otW652Nw0A.mp4','https://images.unsplash.com/photo-1579599709180-375ce2a5db8b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=2125&q=80','5285890803425878758',NULL,10001,NULL,'2020-05-25 15:22:59','2020-05-27 18:18:05','Spring Security',0);
/*!40000 ALTER TABLE `video` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `watch`
--

DROP TABLE IF EXISTS `watch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `watch` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `room_id` int DEFAULT NULL,
  `watch_type` int DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_uid_rid_wt` (`user_id`,`room_id`,`watch_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `watch`
--

LOCK TABLES `watch` WRITE;
/*!40000 ALTER TABLE `watch` DISABLE KEYS */;
INSERT INTO `watch` VALUES (11,10001,2,1,'2021-11-23 23:30:49','2021-11-23 23:30:49'),(12,10001,2,0,'2026-04-20 19:40:32','2026-04-20 19:40:32'),(13,10019,2,0,'2026-04-21 16:45:18','2026-04-21 16:45:18'),(14,10001,1,0,'2026-04-21 17:05:51','2026-04-21 17:05:51'),(15,10019,1,0,'2026-04-21 17:06:53','2026-04-21 17:06:53'),(16,10019,17,0,'2026-04-22 13:40:48','2026-04-22 13:40:48'),(17,10020,18,0,'2026-05-03 15:13:28','2026-05-03 15:13:28'),(18,10001,18,0,'2026-05-03 15:24:26','2026-05-03 15:24:26');
/*!40000 ALTER TABLE `watch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `withdrawal`
--

DROP TABLE IF EXISTS `withdrawal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal` (
  `id` int NOT NULL AUTO_INCREMENT,
  `identity` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '收款账号',
  `identity_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '提现用户名',
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'alipay - wechat',
  `user_id` int DEFAULT NULL COMMENT '用户id',
  `status` int DEFAULT '0' COMMENT '当前状态 0-未完成 1-完成',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `virtual_amount` decimal(10,2) DEFAULT NULL COMMENT '提现金豆',
  `real_amount` decimal(10,2) DEFAULT NULL COMMENT '提现金额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `withdrawal`
--

LOCK TABLES `withdrawal` WRITE;
/*!40000 ALTER TABLE `withdrawal` DISABLE KEYS */;
INSERT INTO `withdrawal` VALUES (1,'123','123','mark','alipay',10001,0,'2020-04-09 15:53:12','2020-04-09 15:53:14',1000.00,100.00),(2,'ihydlk5321@sandbox.com','沙箱环境','提现','alipay',10001,1,'2020-04-09 23:30:46',NULL,100.00,10.00),(3,'ihydlk5321@sandbox.com','沙箱环境','提现','alipay',10001,1,'2020-04-09 23:39:13',NULL,100.00,10.00),(4,'ihydlk5321@sandbox.com','沙箱环境','提现','alipay',10001,1,'2020-04-10 00:12:12',NULL,100.00,10.00),(5,'ihydlk5321@sandbox.com','沙箱环境','提现','alipay',10001,1,'2020-04-10 00:15:09',NULL,100.00,10.00),(6,'ihydlk5321@sandbox.com','沙箱环境','提现','alipay',10001,1,'2020-04-10 00:20:50',NULL,100.00,10.00),(7,'ihydlk5321@sandbox.com','沙箱环境','提现','支付宝',10001,1,'2020-04-11 12:13:43',NULL,10.00,1.00),(8,'ihydlk5321@sandbox.com','沙箱环境','提现','支付宝',10001,1,'2020-05-11 23:54:40','2020-05-11 23:54:40',90.00,18.00),(9,'ihydlk5321@sandbox.com','沙箱环境','提现','支付宝',10001,1,'2020-05-12 00:01:40','2020-05-12 00:01:40',100.00,5.00),(10,'ihydlk5321@sandbox.com','沙箱环境','提现','支付宝',10001,1,'2020-05-12 00:24:03','2020-05-12 00:24:03',440.00,22.00),(11,'ihydlk5321@sandbox.com','沙箱环境','提现','支付宝',10001,1,'2020-05-20 16:07:35','2020-05-20 16:07:35',300.00,15.00);
/*!40000 ALTER TABLE `withdrawal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'ant-live'
--

--
-- Dumping routines for database 'ant-live'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-03 17:12:41
