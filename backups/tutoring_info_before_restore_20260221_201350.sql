-- MySQL dump 10.13  Distrib 8.0.30, for macos12.6 (arm64)
--
-- Host: 127.0.0.1    Database: tutoring_crawler
-- ------------------------------------------------------
-- Server version	8.0.30

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
-- Table structure for table `tutoring_info`
--

DROP TABLE IF EXISTS `tutoring_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tutoring_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_url` varchar(512) NOT NULL COMMENT '文章原始URL',
  `content_block` longtext NOT NULL COMMENT '单条家教信息原始分段内容',
  `city` varchar(64) NOT NULL DEFAULT '' COMMENT '城市',
  `district` varchar(64) NOT NULL DEFAULT '' COMMENT '区县/行政区',
  `grade` varchar(64) NOT NULL DEFAULT '' COMMENT '学员年级',
  `subject` varchar(64) NOT NULL DEFAULT '' COMMENT '辅导科目',
  `address` varchar(256) NOT NULL DEFAULT '' COMMENT '授课地址',
  `time_schedule` varchar(256) NOT NULL DEFAULT '' COMMENT '授课时间安排',
  `salary_text` varchar(128) NOT NULL DEFAULT '' COMMENT '薪酬文本',
  `teacher_requirement` varchar(512) NOT NULL DEFAULT '' COMMENT '教员要求',
  `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文章发布时间',
  `city_snippet` varchar(256) NOT NULL DEFAULT '' COMMENT '城市字段命中片段',
  `district_snippet` varchar(256) NOT NULL DEFAULT '' COMMENT '区县字段命中片段',
  `grade_snippet` varchar(256) NOT NULL DEFAULT '' COMMENT '年级字段命中片段',
  `subject_snippet` varchar(256) NOT NULL DEFAULT '' COMMENT '科目字段命中片段',
  `address_snippet` varchar(512) NOT NULL DEFAULT '' COMMENT '地址字段命中片段',
  `time_schedule_snippet` varchar(512) NOT NULL DEFAULT '' COMMENT '时间字段命中片段',
  `salary_snippet` varchar(256) NOT NULL DEFAULT '' COMMENT '薪酬字段命中片段',
  `teacher_requirement_snippet` varchar(512) NOT NULL DEFAULT '' COMMENT '教员要求字段命中片段',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `source_url` (`source_url`)
) ENGINE=InnoDB AUTO_INCREMENT=23648 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家教结构化信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tutoring_info`
--

LOCK TABLES `tutoring_info` WRITE;
/*!40000 ALTER TABLE `tutoring_info` DISABLE KEYS */;
INSERT INTO `tutoring_info` VALUES (23645,'https://mp.weixin.qq.com/s/mock-a#item-1','A同学数学辅导，周末上课','','','','','','','','','2026-02-17 10:00:00','','','六年级','数学','西湖区','周末','120/小时','女老师','2026-02-21 20:03:55','2026-02-21 20:03:55'),(23646,'https://mp.weixin.qq.com/s/mock-b#item-1','B同学英语提升，平日晚间','','','','','','','','','2026-02-17 12:00:00','','','初二','英语','滨江区','周三晚','150/小时','有经验','2026-02-21 20:03:55','2026-02-21 20:03:55'),(23647,'https://mp.weixin.qq.com/s/mock-c#item-1','C同学物理冲刺，周末下午','','','','','','','','','2026-02-17 09:00:00','','','高三','物理','拱墅区','周末下午','200/小时','男老师','2026-02-21 20:03:55','2026-02-21 20:03:55');
/*!40000 ALTER TABLE `tutoring_info` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-21 20:13:51
