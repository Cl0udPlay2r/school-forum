-- ============================================================
-- 校园论坛（school-forum）数据库结构
-- 仅包含 11 张表结构与 db_topic_type 基础字典，不含用户/帖子/互动数据。
-- 由 mysqldump --no-data 从本地 school_froum 生成；已去掉 AUTO_INCREMENT 当前值。
-- 使用方式：
--   mysql -uroot -p < docs/schema.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `school_froum`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `school_froum`;

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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `role` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `register_time` datetime DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `mute` tinyint DEFAULT NULL COMMENT '禁言',
  `banned` tinyint DEFAULT NULL COMMENT '禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_username` (`username`) USING BTREE,
  UNIQUE KEY `unique_email` (`email`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_account_details` (
  `id` int NOT NULL AUTO_INCREMENT,
  `gender` tinyint DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `qq` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `wx` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `desc` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_account_privacy` (
  `id` int NOT NULL,
  `phone` tinyint DEFAULT NULL,
  `email` tinyint DEFAULT NULL,
  `wx` tinyint DEFAULT NULL,
  `qq` tinyint DEFAULT NULL,
  `gender` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_email_record` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `content` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `status` varchar(4) CHARACTER SET utf8mb3 COLLATE utf8mb3_croatian_ci DEFAULT NULL COMMENT '0为正在发送、1为发送成功、2为发送失败',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_image_store` (
  `uid` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_notification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `content` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `url` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_topic` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_croatian_ci DEFAULT NULL COMMENT '帖子标题',
  `intro` text CHARACTER SET utf8mb3 COLLATE utf8mb3_croatian_ci COMMENT '帖子中的关键信息',
  `content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_croatian_ci COMMENT '帖子内容',
  `uid` int DEFAULT NULL COMMENT '用户id',
  `time` datetime DEFAULT NULL COMMENT '发帖日期',
  `type` int DEFAULT NULL COMMENT '帖子类型',
  `top` tinyint DEFAULT '0' COMMENT '置顶状态;0否，1是',
  `locked` tinyint DEFAULT '0' COMMENT '锁定状态;0否，1是',
  `invisible` tinyint DEFAULT '0' COMMENT '屏蔽状态;0否，1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_topic_comment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `uid` int DEFAULT NULL,
  `tid` int DEFAULT NULL,
  `content` text COLLATE utf8mb3_croatian_ci,
  `time` datetime DEFAULT NULL,
  `quote` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_topic_interact_collect` (
  `tid` int NOT NULL,
  `uid` int NOT NULL,
  `time` datetime DEFAULT NULL,
  PRIMARY KEY (`tid`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_topic_interact_like` (
  `tid` int NOT NULL,
  `uid` int NOT NULL,
  `time` datetime DEFAULT NULL,
  PRIMARY KEY (`tid`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `db_topic_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `desc` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  `color` varchar(255) COLLATE utf8mb3_croatian_ci DEFAULT NULL,
  PRIMARY KEY (`id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_croatian_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


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

INSERT INTO `db_topic_type` VALUES (5,'踩坑记录','将你遇到的坑分享给大家','#3BB62A'),(4,'恋爱官宣','向大家展示你的恋爱成果','#E0CE73'),(3,'问题反馈','反馈你在校园里遇到的问题','#E07373'),(2,'真诚交友','在校园里寻找与自己志同道合的朋友','#CE1EFF'),(1,'日常闲聊','在这里分享你的各种日常','#1E90FF');
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

SET FOREIGN_KEY_CHECKS = 1;
