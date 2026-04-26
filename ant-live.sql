/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : ant-live

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 26/04/2026 09:08:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for auth
-- ----------------------------
DROP TABLE IF EXISTS `auth`;
CREATE TABLE `auth`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `positive_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `reverse_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `card_no` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `status` int NOT NULL,
  `hand_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `operator` int NULL DEFAULT NULL,
  `reject_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of auth
-- ----------------------------
INSERT INTO `auth` VALUES (9, 10001, '番茄蛋', 'http://image.imhtb.cn/avatar.png', 'http://image.imhtb.cn/avatar.png', '777888333378777727', 1, 'http://image.imhtb.cn/avatar.png', '2020-05-20 15:50:13', '2020-05-22 13:37:29', 0, NULL);

-- ----------------------------
-- Table structure for ban_record
-- ----------------------------
DROP TABLE IF EXISTS `ban_record`;
CREATE TABLE `ban_record`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int NULL DEFAULT NULL,
  `resume_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `status` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ban_record
-- ----------------------------
INSERT INTO `ban_record` VALUES (1, 1001, '2020-05-23 18:46:15', '2020-05-23 18:46:17', '2020-05-23 18:46:19', '封禁原因', '2020-05-23 18:46:26', '备注', 0);
INSERT INTO `ban_record` VALUES (2, 16, '2020-05-27 16:00:00', '2020-05-27 20:54:14', NULL, '涉黄', '2020-05-27 20:54:14', '手动恢复', 1);
INSERT INTO `ban_record` VALUES (3, 17, '2026-05-02 11:26:02', '2026-04-25 11:26:02', '2026-04-25 11:26:02', 'Live stopped: violent behavior detected', '2026-04-25 11:26:02', NULL, 0);

-- ----------------------------
-- Table structure for bill
-- ----------------------------
DROP TABLE IF EXISTS `bill`;
CREATE TABLE `bill`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `bill_change` decimal(10, 2) NOT NULL,
  `type` int NOT NULL,
  `balance` decimal(10, 2) NOT NULL,
  `ip` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `order_no` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK_BILL_USER_ID`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FK_BILL_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of bill
-- ----------------------------
INSERT INTO `bill` VALUES (1, 10001, 200.00, 0, 200.00, '1', NULL, '2020-03-16 11:15:50', NULL, '587946745648');
INSERT INTO `bill` VALUES (2, 10001, -2.00, 1, 198.00, '1', NULL, '2020-03-11 11:15:52', NULL, '323934545648');
INSERT INTO `bill` VALUES (3, 10002, 0.00, 0, 0.00, '1', NULL, '2020-03-25 01:52:17', NULL, '587946745648');
INSERT INTO `bill` VALUES (6, 10001, 88.88, 1, 109.12, NULL, '向直播间2赠送礼物', '2020-03-25 01:54:15', NULL, '587946745648');
INSERT INTO `bill` VALUES (7, 10002, 88.88, 0, 88.88, NULL, '收获礼物', '2020-03-25 01:54:15', NULL, '587946745648');
INSERT INTO `bill` VALUES (8, 10001, 99.99, 1, 9.13, NULL, '向直播间2赠送礼物', '2020-03-25 01:54:23', NULL, '587946745648');
INSERT INTO `bill` VALUES (9, 10002, 99.99, 0, 188.87, NULL, '收获礼物', '2020-03-25 01:54:23', NULL, '587946745648');
INSERT INTO `bill` VALUES (10, 10001, 100000.00, 0, 100000.00, NULL, NULL, '2020-04-06 19:54:09', NULL, '587946745648');
INSERT INTO `bill` VALUES (11, 10001, 88000.00, 1, 12000.00, NULL, '赠送礼物', '2020-04-06 19:54:37', NULL, NULL);
INSERT INTO `bill` VALUES (12, 10002, 88000.00, 0, 88188.87, NULL, '收获礼物', '2020-04-06 19:54:37', NULL, NULL);
INSERT INTO `bill` VALUES (13, 10001, 1000.00, 1, 11000.00, NULL, '赠送礼物', '2020-04-06 19:55:04', NULL, NULL);
INSERT INTO `bill` VALUES (14, 10002, 1000.00, 0, 89188.87, NULL, '收获礼物', '2020-04-06 19:55:04', NULL, NULL);
INSERT INTO `bill` VALUES (15, 10001, -100.00, 1, 10900.00, NULL, '提现', '2020-04-08 18:34:42', NULL, '123118471236123');
INSERT INTO `bill` VALUES (16, 10001, -1000.00, 1, 9900.00, NULL, '提现', '2020-04-08 19:12:52', NULL, '123118471236123');
INSERT INTO `bill` VALUES (17, 10001, -1000.00, 1, 8900.00, NULL, '提现', '2020-04-08 19:13:36', NULL, '123118471236123');
INSERT INTO `bill` VALUES (18, 10001, -1000.00, 1, 7900.00, NULL, '提现', '2020-04-08 19:15:08', NULL, '123118471236123');
INSERT INTO `bill` VALUES (19, 10001, -1000.00, 1, 6900.00, NULL, '提现', '2020-04-08 19:16:49', NULL, '123118471236123');
INSERT INTO `bill` VALUES (20, 10001, -1000.00, 1, 5900.00, NULL, '提现', '2020-04-08 19:27:05', NULL, '123118471236123');
INSERT INTO `bill` VALUES (21, 10001, -1000.00, 1, 4900.00, NULL, '提现', '2020-04-08 19:30:10', NULL, '1a40b7bceb8270e7d96a94');
INSERT INTO `bill` VALUES (22, 10001, -1000.00, 1, 3900.00, NULL, '提现', '2020-04-08 19:31:02', NULL, '5742b0ae83672c63b992ab');
INSERT INTO `bill` VALUES (23, 10001, -1000.00, 1, 2900.00, NULL, '提现', '2020-04-08 19:31:19', NULL, '274a41bc9d4adf6aa4188f');
INSERT INTO `bill` VALUES (24, 10001, -1000.00, 1, 1900.00, NULL, '提现', '2020-04-08 19:31:25', NULL, '2540099e4277d072e2a816');
INSERT INTO `bill` VALUES (25, 10001, -1000.00, 1, 900.00, NULL, '提现', '2020-04-08 19:31:28', NULL, '044cdabc0de3367d6675f3');
INSERT INTO `bill` VALUES (26, 10001, -100.00, 1, 800.00, NULL, '提现', '2020-04-09 23:20:24', NULL, '9644fd801615909ecccb0c');
INSERT INTO `bill` VALUES (27, 10001, -100.00, 1, 700.00, NULL, '提现', '2020-04-09 23:30:46', NULL, '014dada040cef8aebe4292');
INSERT INTO `bill` VALUES (28, 10001, -100.00, 1, 600.00, NULL, '提现', '2020-04-09 23:39:13', NULL, '414c7b937cc84bde6042a2');
INSERT INTO `bill` VALUES (29, 10001, -100.00, 1, 500.00, NULL, '提现', '2020-04-10 00:12:12', NULL, '7d4be2a2e4aacdaed5c331');
INSERT INTO `bill` VALUES (30, 10001, -100.00, 1, 400.00, NULL, '提现', '2020-04-10 00:15:09', NULL, 'e647c0947baa3f69cb017e');
INSERT INTO `bill` VALUES (31, 10001, -100.00, 1, 300.00, NULL, '提现', '2020-04-10 00:20:50', NULL, '694e7b96bd2528308c8f18');
INSERT INTO `bill` VALUES (32, 10001, -10.00, 1, 290.00, NULL, '提现', '2020-04-11 12:13:43', NULL, '944608a5d6cc68aeefddf1');
INSERT INTO `bill` VALUES (35, 10001, -10.00, 1, 280.00, NULL, '赠送礼物', '2020-04-11 13:18:32', NULL, NULL);
INSERT INTO `bill` VALUES (36, 10001, 10.00, 0, 300.00, NULL, '收获礼物', '2020-04-11 13:18:32', NULL, NULL);
INSERT INTO `bill` VALUES (37, 10001, -10.00, 1, 290.00, NULL, '赠送礼物', '2020-04-11 13:19:11', NULL, NULL);
INSERT INTO `bill` VALUES (38, 10001, 10.00, 0, 310.00, NULL, '收获礼物', '2020-04-11 13:19:11', NULL, NULL);
INSERT INTO `bill` VALUES (39, 10001, -10.00, 1, 300.00, NULL, '赠送礼物', '2020-04-11 13:19:30', NULL, NULL);
INSERT INTO `bill` VALUES (40, 10001, 10.00, 0, 320.00, NULL, '收获礼物', '2020-04-11 13:19:30', NULL, NULL);
INSERT INTO `bill` VALUES (41, 10001, -10.00, 1, 310.00, NULL, '赠送礼物', '2020-04-11 13:20:24', NULL, NULL);
INSERT INTO `bill` VALUES (42, 10001, 10.00, 0, 330.00, NULL, '收获礼物', '2020-04-11 13:20:24', NULL, NULL);
INSERT INTO `bill` VALUES (43, 10001, -10.00, 1, 320.00, NULL, '赠送礼物', '2020-04-11 13:20:31', NULL, NULL);
INSERT INTO `bill` VALUES (44, 10001, 10.00, 0, 340.00, NULL, '收获礼物', '2020-04-11 13:20:31', NULL, NULL);
INSERT INTO `bill` VALUES (45, 10001, -10.00, 1, 330.00, NULL, '赠送礼物', '2020-04-11 13:23:04', NULL, NULL);
INSERT INTO `bill` VALUES (46, 10001, 10.00, 0, 350.00, NULL, '收获礼物', '2020-04-11 13:23:04', NULL, NULL);
INSERT INTO `bill` VALUES (47, 10007, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-09 11:58:29', '2020-05-09 11:58:29', NULL);
INSERT INTO `bill` VALUES (48, 10001, 640.00, 0, 990.00, NULL, NULL, '2020-05-11 23:53:30', '2020-05-11 23:53:30', '724847b0459cbc32954b0b');
INSERT INTO `bill` VALUES (49, 10001, -90.00, 1, 900.00, NULL, '提现', '2020-05-11 23:54:40', '2020-05-11 23:54:40', '564032b50b2b30149a46d7');
INSERT INTO `bill` VALUES (50, 10001, -100.00, 1, 800.00, NULL, '提现', '2020-05-12 00:01:40', '2020-05-12 00:01:40', '61459ea8206ac4aef665f1');
INSERT INTO `bill` VALUES (51, 10001, 640.00, 0, 1440.00, NULL, NULL, '2020-05-12 00:23:28', '2020-05-12 00:23:28', '0843b2bfd8cf1d10b113a7');
INSERT INTO `bill` VALUES (52, 10001, -440.00, 1, 1000.00, NULL, '提现', '2020-05-12 00:24:03', '2020-05-12 00:24:03', '664649aab9ba0c5e35a7f1');
INSERT INTO `bill` VALUES (53, 10008, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-13 17:34:19', '2020-05-13 17:34:19', NULL);
INSERT INTO `bill` VALUES (54, 10009, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-18 12:52:48', '2020-05-18 12:52:48', NULL);
INSERT INTO `bill` VALUES (55, 10001, 640.00, 0, 1640.00, NULL, NULL, '2020-05-20 16:06:28', '2020-05-20 16:06:28', '0f4a908469d9b1807b5084');
INSERT INTO `bill` VALUES (56, 10001, -300.00, 1, 1340.00, NULL, '提现', '2020-05-20 16:07:35', '2020-05-20 16:07:35', 'ef4b5092e41722a0da8805');
INSERT INTO `bill` VALUES (63, 10001, -10.00, 1, 1330.00, NULL, '赠送礼物', '2020-05-25 22:47:13', '2020-05-25 22:47:13', NULL);
INSERT INTO `bill` VALUES (64, 10002, 10.00, 0, 89198.87, NULL, '收获礼物', '2020-05-25 22:47:13', '2020-05-25 22:47:13', NULL);
INSERT INTO `bill` VALUES (65, 10002, -1000.00, 0, 88198.87, NULL, '视频打赏', '2020-05-26 11:26:58', '2020-05-26 11:26:58', 'a14142a0602b08ca2613e5');
INSERT INTO `bill` VALUES (66, 10001, 1000.00, 0, 2330.00, NULL, '视频打赏', '2020-05-26 11:26:58', '2020-05-26 11:26:58', '734696a97e485dde8999df');
INSERT INTO `bill` VALUES (67, 10001, -100.00, 0, 2230.00, NULL, '直播打赏', '2020-05-26 11:29:53', '2020-05-26 11:29:53', '8f410f8f91f7df66297dc6');
INSERT INTO `bill` VALUES (68, 10002, 100.00, 0, 88298.87, NULL, '直播打赏', '2020-05-26 11:29:53', '2020-05-26 11:29:53', '5348a6b0618dc676c07d5d');
INSERT INTO `bill` VALUES (69, 10001, -100.00, 0, 2130.00, NULL, '直播打赏', '2020-05-26 11:30:09', '2020-05-26 11:30:09', '304b5ba33afa93c44b8d20');
INSERT INTO `bill` VALUES (70, 10002, 100.00, 0, 88398.87, NULL, '直播打赏', '2020-05-26 11:30:09', '2020-05-26 11:30:09', '274d5cb27bbb251f941688');
INSERT INTO `bill` VALUES (71, 10001, -1000.00, 0, 1130.00, NULL, '直播打赏', '2020-05-26 11:30:43', '2020-05-26 11:30:43', 'b64681b2df4c2dc93737e8');
INSERT INTO `bill` VALUES (72, 10002, 1000.00, 0, 89398.87, NULL, '直播打赏', '2020-05-26 11:30:43', '2020-05-26 11:30:43', '2142ae8f0a9ed430c1d8d2');
INSERT INTO `bill` VALUES (73, 10010, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 16:58:14', '2020-05-27 16:58:14', NULL);
INSERT INTO `bill` VALUES (74, 10011, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:01:37', '2020-05-27 17:01:37', NULL);
INSERT INTO `bill` VALUES (75, 10012, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:03:04', '2020-05-27 17:03:04', NULL);
INSERT INTO `bill` VALUES (76, 10013, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:04:30', '2020-05-27 17:04:30', NULL);
INSERT INTO `bill` VALUES (77, 10014, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:06:09', '2020-05-27 17:06:09', NULL);
INSERT INTO `bill` VALUES (78, 10015, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:08:06', '2020-05-27 17:08:06', NULL);
INSERT INTO `bill` VALUES (79, 10016, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:10:31', '2020-05-27 17:10:31', NULL);
INSERT INTO `bill` VALUES (80, 10017, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:14:41', '2020-05-27 17:14:41', NULL);
INSERT INTO `bill` VALUES (81, 10018, 0.00, 0, 0.00, NULL, '初始化账单', '2020-05-27 17:57:45', '2020-05-27 17:57:45', NULL);
INSERT INTO `bill` VALUES (82, 10001, -10.00, 1, 1120.00, NULL, '视频打赏', '2021-11-23 23:29:42', '2021-11-23 23:29:42', 'e5452c963e31822fb6fd0e');
INSERT INTO `bill` VALUES (83, 10001, 10.00, 0, 1140.00, NULL, '视频打赏', '2021-11-23 23:29:42', '2021-11-23 23:29:42', '204d4c9fd63d4cb0ac52cf');

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `sort` int NULL DEFAULT NULL,
  `disabled` int NULL DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `parent_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (1, '游戏直播', 1, 0, 0, '2020-04-19 01:33:15', '2020-04-19 01:33:17', NULL);
INSERT INTO `category` VALUES (2, '娱乐直播', 1, 0, 0, '2020-04-19 01:33:35', '2020-04-19 01:33:37', NULL);

-- ----------------------------
-- Table structure for live_detect
-- ----------------------------
DROP TABLE IF EXISTS `live_detect`;
CREATE TABLE `live_detect`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int NULL DEFAULT NULL,
  `type` int NULL DEFAULT NULL,
  `confidence` int NULL DEFAULT NULL,
  `img` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `normal_score` int NULL DEFAULT NULL,
  `hot_score` int NULL DEFAULT NULL,
  `porn_score` int NULL DEFAULT NULL,
  `level` int NULL DEFAULT NULL,
  `polity_score` int NULL DEFAULT NULL,
  `illegal_score` int NULL DEFAULT NULL,
  `terror_score` int NULL DEFAULT NULL,
  `handle_status` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `screenshot_time` int NULL DEFAULT NULL,
  `resume_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of live_detect
-- ----------------------------
INSERT INTO `live_detect` VALUES (1, 1, 1, 99, 'http://image.imhtb.cn/avatar.png', 1, 0, 99, 0, 0, 0, 0, 1, '2020-05-13 18:17:26', NULL, 1589365045, '2020-05-14 02:17:26');
INSERT INTO `live_detect` VALUES (2, 3, 1, 99, 'http://image.imhtb.cn/avatar.png', 1, 0, 99, 0, 0, 0, 0, 1, '2020-05-13 18:23:36', NULL, 1589365415, '2020-05-14 02:23:36');

-- ----------------------------
-- Table structure for live_info
-- ----------------------------
DROP TABLE IF EXISTS `live_info`;
CREATE TABLE `live_info`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `room_id` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `status` int NULL DEFAULT NULL,
  `click_count` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `dan_mu_count` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `present_count` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK_ROOM_INFO_ROOM_ID`(`room_id` ASC) USING BTREE,
  CONSTRAINT `FK_ROOM_INFO_ROOM_ID` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 61 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of live_info
-- ----------------------------
INSERT INTO `live_info` VALUES (8, '2020-03-05 18:00:30', '2020-03-05 18:04:41', 1, 10001, '2020-03-05 18:00:30', '2020-03-05 18:04:41', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (9, '2020-03-05 18:05:37', '2020-03-05 18:14:08', 1, 10001, '2020-03-05 18:05:37', '2020-03-05 18:14:08', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (10, '2020-03-18 16:48:33', '2020-03-18 16:49:29', 1, 10001, '2020-03-18 16:48:33', '2020-03-18 16:49:29', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (11, '2020-03-18 16:50:57', '2020-03-18 16:51:18', 1, 10001, '2020-03-18 16:50:57', '2020-03-18 16:51:18', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (12, '2020-03-18 17:43:38', '2020-03-18 17:53:17', 1, 10001, '2020-03-18 17:43:38', '2020-03-18 17:53:17', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (13, '2020-05-10 23:52:49', '2020-05-10 23:54:41', 1, 10001, '2020-05-10 23:52:49', '2020-05-10 23:54:41', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (14, '2020-05-10 23:55:33', '2020-05-10 23:58:12', 1, 10001, '2020-05-10 23:55:33', '2020-05-10 23:58:12', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (15, '2020-05-13 16:28:40', '2020-05-13 16:32:12', 1, 10001, '2020-05-13 16:28:40', '2020-05-13 16:32:12', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (16, '2020-05-13 16:36:12', '2020-05-13 16:38:19', 1, 10001, '2020-05-13 16:36:12', '2020-05-13 16:38:19', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (17, '2020-05-13 16:54:04', '2020-05-13 17:02:34', 1, 10001, '2020-05-13 16:54:04', '2020-05-13 17:02:34', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (18, '2020-05-13 17:12:31', '2020-05-13 17:16:51', 1, 10001, '2020-05-13 17:12:31', '2020-05-13 17:16:51', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (19, '2020-05-13 17:17:25', '2020-05-13 17:19:32', 1, 10001, '2020-05-13 17:17:25', '2020-05-13 17:19:32', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (20, '2020-05-13 17:20:47', '2020-05-13 17:22:32', 1, 10001, '2020-05-13 17:20:47', '2020-05-13 17:22:32', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (21, '2020-05-13 17:27:37', '2020-05-13 17:30:14', 1, 10001, '2020-05-13 17:27:37', '2020-05-13 17:30:14', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (22, '2020-05-13 17:34:48', '2020-05-13 17:37:32', 1, 10001, '2020-05-13 17:34:48', '2020-05-13 17:37:32', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (23, '2020-05-13 17:40:28', '2020-05-13 17:45:28', 1, 10001, '2020-05-13 17:40:28', '2020-05-13 17:45:28', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (24, '2020-05-13 17:53:46', '2020-05-13 17:54:20', 1, 10001, '2020-05-13 17:53:46', '2020-05-13 17:54:20', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (25, '2020-05-13 18:10:54', '2020-05-13 18:11:36', 1, 10001, '2020-05-13 18:10:54', '2020-05-13 18:11:36', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (26, '2020-05-13 18:13:22', '2020-05-13 18:13:59', 1, 10001, '2020-05-13 18:13:22', '2020-05-13 18:13:59', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (27, '2020-05-13 18:15:06', '2020-05-13 18:15:15', 1, 10001, '2020-05-13 18:15:06', '2020-05-13 18:15:15', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (28, '2020-05-13 18:15:55', '2020-05-13 18:16:02', 1, 10001, '2020-05-13 18:15:55', '2020-05-13 18:16:02', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (29, '2020-05-13 18:17:21', '2020-05-13 18:17:51', 1, 10001, '2020-05-13 18:17:21', '2020-05-13 18:17:51', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (30, '2020-05-13 18:23:32', '2020-05-13 18:23:43', 1, 10001, '2020-05-13 18:23:32', '2020-05-13 18:23:43', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (31, '2020-05-17 10:56:34', '2020-05-17 10:59:52', 1, 10001, '2020-05-17 10:56:34', '2020-05-17 10:59:52', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (32, '2020-05-19 09:22:12', '2020-05-19 09:23:01', 1, 10001, '2020-05-19 09:22:12', '2020-05-19 09:23:01', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (33, '2020-05-20 15:55:07', '2020-05-20 15:57:59', 1, 10001, '2020-05-20 15:55:07', '2020-05-20 15:57:59', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (34, '2020-05-20 15:58:41', '2020-05-20 15:59:36', 1, 10001, '2020-05-20 15:58:41', '2020-05-20 15:59:36', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (35, '2020-05-20 16:59:16', '2020-05-20 16:59:30', 1, 10001, '2020-05-20 16:59:16', '2020-05-20 16:59:30', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (36, '2026-04-24 16:03:55', '2026-04-24 16:04:42', 17, 10019, '2026-04-24 16:03:55', '2026-04-24 16:03:55', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (37, '2026-04-24 16:04:47', '2026-04-24 16:10:34', 17, 10019, '2026-04-24 16:04:47', '2026-04-24 16:04:47', 1, '1', '0', '0');
INSERT INTO `live_info` VALUES (38, '2026-04-24 16:26:23', '2026-04-24 16:27:14', 17, 10019, '2026-04-24 16:26:23', '2026-04-24 16:26:23', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (39, '2026-04-24 22:55:24', '2026-04-24 22:56:10', 17, 10019, '2026-04-24 22:55:24', '2026-04-24 22:55:24', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (40, '2026-04-24 23:00:42', '2026-04-24 23:03:02', 17, 10019, '2026-04-24 23:00:42', '2026-04-24 23:00:42', 1, '1', '0', '0');
INSERT INTO `live_info` VALUES (41, '2026-04-24 23:03:03', '2026-04-24 23:03:39', 17, 10019, '2026-04-24 23:03:03', '2026-04-24 23:03:03', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (42, '2026-04-24 23:20:06', '2026-04-24 23:23:02', 17, 10019, '2026-04-24 23:20:06', '2026-04-24 23:20:06', 1, '1', '0', '0');
INSERT INTO `live_info` VALUES (43, '2026-04-25 09:48:18', '2026-04-25 09:48:42', 17, 10019, '2026-04-25 09:48:18', '2026-04-25 09:48:18', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (44, '2026-04-25 09:50:29', '2026-04-25 09:50:33', 17, 10019, '2026-04-25 09:50:29', '2026-04-25 09:50:29', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (45, '2026-04-25 09:50:35', '2026-04-25 10:54:41', 17, 10019, '2026-04-25 09:50:35', '2026-04-25 09:50:35', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (46, '2026-04-25 11:24:56', '2026-04-25 11:26:02', 17, 10019, '2026-04-25 11:24:56', '2026-04-25 11:24:56', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (47, '2026-04-25 11:37:36', '2026-04-25 11:37:40', 17, 10019, '2026-04-25 11:37:36', '2026-04-25 11:37:36', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (48, '2026-04-25 11:40:20', '2026-04-25 11:40:30', 17, 10019, '2026-04-25 11:40:20', '2026-04-25 11:40:20', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (49, '2026-04-25 11:40:54', '2026-04-25 11:41:36', 17, 10019, '2026-04-25 11:40:54', '2026-04-25 11:40:54', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (50, '2026-04-25 11:42:01', '2026-04-25 11:43:02', 17, 10019, '2026-04-25 11:42:01', '2026-04-25 11:42:01', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (51, '2026-04-25 11:43:39', '2026-04-25 11:44:36', 17, 10019, '2026-04-25 11:43:39', '2026-04-25 11:43:39', 1, '1', '0', '0');
INSERT INTO `live_info` VALUES (52, '2026-04-25 11:49:29', '2026-04-25 11:50:35', 17, 10019, '2026-04-25 11:49:29', '2026-04-25 11:49:29', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (53, '2026-04-25 11:50:45', NULL, 1, 10001, '2026-04-25 11:50:45', '2026-04-25 11:50:45', 0, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (54, '2026-04-25 11:50:58', '2026-04-25 11:51:34', 17, 10019, '2026-04-25 11:50:58', '2026-04-25 11:50:58', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (55, '2026-04-25 11:52:06', '2026-04-25 11:52:50', 17, 10019, '2026-04-25 11:52:06', '2026-04-25 11:52:06', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (56, '2026-04-25 18:38:25', '2026-04-25 18:38:35', 17, 10019, '2026-04-25 18:38:25', '2026-04-25 18:38:25', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (57, '2026-04-26 08:21:26', '2026-04-26 08:21:38', 17, 10019, '2026-04-26 08:21:26', '2026-04-26 08:21:26', 1, '0', '0', '0');
INSERT INTO `live_info` VALUES (58, '2026-04-26 08:29:20', '2026-04-26 08:29:31', 17, 10019, '2026-04-26 08:29:20', '2026-04-26 08:29:20', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (59, '2026-04-26 08:30:23', '2026-04-26 08:32:47', 17, 10019, '2026-04-26 08:30:23', '2026-04-26 08:30:23', 1, NULL, NULL, NULL);
INSERT INTO `live_info` VALUES (60, '2026-04-26 08:32:51', NULL, 17, 10019, '2026-04-26 08:32:51', '2026-04-26 08:32:51', 0, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for menu
-- ----------------------------
DROP TABLE IF EXISTS `menu`;
CREATE TABLE `menu`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `menu_index` int NULL DEFAULT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `pid` int NULL DEFAULT 0,
  `sort` int NULL DEFAULT NULL,
  `hidden` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of menu
-- ----------------------------
INSERT INTO `menu` VALUES (1, 1, 'el-icon-data-board', 'dashboard', '首页', 0, 1, 0, NULL, NULL);
INSERT INTO `menu` VALUES (2, 2, 'el-icon-user', 'user-manage', '会员中心', 0, 2, 0, NULL, NULL);
INSERT INTO `menu` VALUES (3, 3, 'el-icon-coordinate', 'user-auth', '身份验证', 0, 3, 0, NULL, NULL);
INSERT INTO `menu` VALUES (4, 4, 'el-icon-bangzhu', 'live-room-manage', '直播管理', 16, 4, 0, NULL, NULL);
INSERT INTO `menu` VALUES (5, 5, 'el-icon-data-analysis', 'live-info-manage', '直播数据', 16, 5, 0, NULL, NULL);
INSERT INTO `menu` VALUES (6, 6, 'el-icon-menu', 'system-settings', '系统设置', 0, 6, 1, NULL, NULL);
INSERT INTO `menu` VALUES (7, 7, 'el-icon-data-analysis', 'data-analysis', '数据统计', 0, 90, 0, NULL, '2020-05-23 19:41:03');
INSERT INTO `menu` VALUES (8, 8, 'el-icon-goods', 'present-manage', '礼物配置', 0, 8, 0, NULL, '2020-05-27 11:18:51');
INSERT INTO `menu` VALUES (9, 9, 'el-icon-s-shop', 'live-ban-manage', '小黑屋', 16, 9, 1, NULL, '2020-05-23 19:48:26');
INSERT INTO `menu` VALUES (10, 10, 'el-icon-chat-line-round', 'message-push', '消息推送', 0, 88, 0, NULL, '2020-05-09 11:25:27');
INSERT INTO `menu` VALUES (11, 11, 'el-icon-data-analysis', 'system-monitor-host', '服务监控', 12, 11, 0, NULL, NULL);
INSERT INTO `menu` VALUES (12, 12, 'el-icon-warning-outline', 'system-monitor', '系统监控', 0, 18, 0, NULL, '2020-05-27 11:18:18');
INSERT INTO `menu` VALUES (13, 13, 'el-icon-data-analysis', 'system-manage', '系统管理', 0, 13, 0, NULL, NULL);
INSERT INTO `menu` VALUES (14, 14, 'el-icon-data-analysis', 'system-manage-menu', '菜单管理', 13, 14, 0, NULL, NULL);
INSERT INTO `menu` VALUES (15, 15, 'el-icon-data-analysis', 'system-manage-role', '角色管理', 13, 15, 0, NULL, NULL);
INSERT INTO `menu` VALUES (16, 16, 'el-icon-video-camera', 'live-center', '直播中心', 0, 16, 0, NULL, '2020-05-09 11:25:52');
INSERT INTO `menu` VALUES (17, 17, 'el-icon-data-board', 'live-detect', '截图检测', 16, 6, 0, '2020-05-08 14:36:26', '2020-05-23 19:47:54');
INSERT INTO `menu` VALUES (18, 18, 'el-icon-data-analysis', 'bill', '账单中心', 0, 4, 0, '2020-05-09 11:11:18', NULL);
INSERT INTO `menu` VALUES (19, 19, 'el-icon-user', 'user-role-manage', '角色分配', 13, 1, 0, '2020-05-09 17:24:39', '2020-05-19 11:39:04');
INSERT INTO `menu` VALUES (20, 20, 'el-icon-data-board', 'snapshot-templates', '鉴黄模板', 16, 1, 0, '2020-05-22 14:32:58', '2020-05-23 19:15:10');
INSERT INTO `menu` VALUES (21, 21, 'el-icon-warning-outline', 'ban-record', '封禁记录', 16, 1, 0, '2020-05-23 18:27:47', '2020-05-23 19:15:05');
INSERT INTO `menu` VALUES (22, NULL, 'el-icon-data-analysis', 'vod', '视频中心', 0, 17, 0, '2020-05-25 16:13:02', '2020-05-27 11:17:53');
INSERT INTO `menu` VALUES (23, NULL, 'el-icon-data-analysis', 'video-manage', '视频管理', 22, 1, 0, '2020-05-25 16:13:51', '2020-05-27 10:25:21');
INSERT INTO `menu` VALUES (24, NULL, 'el-icon-data-analysis', 'vod-list', '稿件列表', 22, 2, 0, '2020-05-27 11:51:19', NULL);

-- ----------------------------
-- Table structure for present
-- ----------------------------
DROP TABLE IF EXISTS `present`;
CREATE TABLE `present`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `price` decimal(10, 2) NOT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `sort` int NULL DEFAULT 0,
  `disabled` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of present
-- ----------------------------
INSERT INTO `present` VALUES (1, '火箭', 'http://image.imhtb.cn/飞机.png', 10.00, '2020-02-26 18:20:48', '2020-05-25 22:34:46', 0, 0);
INSERT INTO `present` VALUES (2, '飞机', 'http://image.imhtb.cn/飞机1.png', 88000.00, '2020-03-02 10:01:35', '2020-05-25 22:34:39', 3, 0);

-- ----------------------------
-- Table structure for present_reward
-- ----------------------------
DROP TABLE IF EXISTS `present_reward`;
CREATE TABLE `present_reward`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `from_id` int NULL DEFAULT NULL,
  `to_id` int NULL DEFAULT NULL,
  `room_id` int NULL DEFAULT NULL,
  `video_id` int NULL DEFAULT NULL,
  `present_id` int NULL DEFAULT NULL,
  `number` int NULL DEFAULT NULL,
  `unit_price` decimal(10, 2) NULL DEFAULT NULL,
  `total_price` decimal(10, 2) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `type` int NULL DEFAULT NULL COMMENT '0-为直播间礼物 1-为视频打赏礼物',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK_ROOM_PRESENT_ROOM_ID`(`room_id` ASC) USING BTREE,
  CONSTRAINT `FK_ROOM_PRESENT_ROOM_ID` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of present_reward
-- ----------------------------
INSERT INTO `present_reward` VALUES (5, 10002, 10001, 2, NULL, 2, 1, 88000.00, 88000.00, '2020-04-06 19:54:37', NULL, NULL);
INSERT INTO `present_reward` VALUES (6, 10002, 10001, 2, NULL, 1, 1, 1000.00, 1000.00, '2020-04-06 19:55:04', NULL, NULL);
INSERT INTO `present_reward` VALUES (8, 10001, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2020-04-11 13:18:32', NULL, NULL);
INSERT INTO `present_reward` VALUES (9, 10001, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2020-04-11 13:19:11', NULL, NULL);
INSERT INTO `present_reward` VALUES (10, 10001, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2020-04-11 13:19:30', NULL, NULL);
INSERT INTO `present_reward` VALUES (11, 10001, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2020-04-11 13:20:24', NULL, NULL);
INSERT INTO `present_reward` VALUES (12, 10001, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2020-04-11 13:20:31', NULL, NULL);
INSERT INTO `present_reward` VALUES (13, 10001, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2020-04-11 13:23:04', NULL, NULL);
INSERT INTO `present_reward` VALUES (17, 10001, 10002, 2, NULL, 1, 1, 10.00, 10.00, '2020-05-25 22:47:13', NULL, 1);
INSERT INTO `present_reward` VALUES (18, 10002, 10001, NULL, 1, 1, 100, 10.00, 1000.00, '2020-05-26 11:26:58', NULL, 1);
INSERT INTO `present_reward` VALUES (19, 10001, 10002, 2, NULL, 1, 10, 10.00, 100.00, '2020-05-26 11:29:53', NULL, 0);
INSERT INTO `present_reward` VALUES (20, 10001, 10002, 2, NULL, 1, 10, 10.00, 100.00, '2020-05-26 11:30:09', NULL, 0);
INSERT INTO `present_reward` VALUES (21, 10001, 10002, 2, NULL, 1, 100, 10.00, 1000.00, '2020-05-26 11:30:43', NULL, 0);
INSERT INTO `present_reward` VALUES (22, 10001, 10001, NULL, 2, 1, 1, 10.00, 10.00, '2021-11-23 23:29:42', NULL, 1);
INSERT INTO `present_reward` VALUES (23, 10019, 10001, 1, NULL, 1, 10, 10.00, 100.00, '2026-04-25 19:41:55', NULL, 0);
INSERT INTO `present_reward` VALUES (24, 10019, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2026-04-25 19:53:06', NULL, 0);
INSERT INTO `present_reward` VALUES (25, 10001, 10019, 17, NULL, 1, 1, 10.00, 10.00, '2026-04-26 08:34:42', NULL, 0);
INSERT INTO `present_reward` VALUES (26, 10019, 10001, 1, NULL, 1, 1, 10.00, 10.00, '2026-04-26 08:36:37', NULL, 0);
INSERT INTO `present_reward` VALUES (27, 10019, 10001, 1, NULL, 1, 10, 10.00, 100.00, '2026-04-26 08:39:14', NULL, 0);

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `level` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `permission` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, '超级管理员', 1, '2020-04-30 13:57:24', '2020-04-30 13:57:27', 'ROLE_ROOT');
INSERT INTO `role` VALUES (2, '直播管理员', 2, '2020-04-30 13:57:46', '2020-04-30 13:57:48', 'ROLE_LIVE');
INSERT INTO `role` VALUES (100, '普通会员', 8, '2020-04-30 15:16:56', '2020-04-30 15:16:58', 'ROLE_COMMON');
INSERT INTO `role` VALUES (101, '身份认证管理员', 2, '2020-05-09 11:21:03', NULL, 'ROLE_AUTH');

-- ----------------------------
-- Table structure for role_menu
-- ----------------------------
DROP TABLE IF EXISTS `role_menu`;
CREATE TABLE `role_menu`  (
  `role_id` int NOT NULL,
  `menu_id` int NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE,
  INDEX `FK_ROLE_MENU_MENU_ID`(`menu_id` ASC) USING BTREE,
  CONSTRAINT `FK_ROLE_MENU_MENU_ID` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_ROLE_MENU_ROLE_ID` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of role_menu
-- ----------------------------
INSERT INTO `role_menu` VALUES (1, 1);
INSERT INTO `role_menu` VALUES (2, 1);
INSERT INTO `role_menu` VALUES (101, 1);
INSERT INTO `role_menu` VALUES (1, 2);
INSERT INTO `role_menu` VALUES (1, 3);
INSERT INTO `role_menu` VALUES (101, 3);
INSERT INTO `role_menu` VALUES (1, 4);
INSERT INTO `role_menu` VALUES (2, 4);
INSERT INTO `role_menu` VALUES (1, 5);
INSERT INTO `role_menu` VALUES (2, 5);
INSERT INTO `role_menu` VALUES (1, 7);
INSERT INTO `role_menu` VALUES (1, 8);
INSERT INTO `role_menu` VALUES (1, 9);
INSERT INTO `role_menu` VALUES (2, 9);
INSERT INTO `role_menu` VALUES (1, 10);
INSERT INTO `role_menu` VALUES (1, 11);
INSERT INTO `role_menu` VALUES (1, 12);
INSERT INTO `role_menu` VALUES (1, 13);
INSERT INTO `role_menu` VALUES (1, 14);
INSERT INTO `role_menu` VALUES (1, 15);
INSERT INTO `role_menu` VALUES (1, 16);
INSERT INTO `role_menu` VALUES (2, 16);
INSERT INTO `role_menu` VALUES (1, 17);
INSERT INTO `role_menu` VALUES (2, 17);
INSERT INTO `role_menu` VALUES (1, 18);
INSERT INTO `role_menu` VALUES (1, 19);
INSERT INTO `role_menu` VALUES (1, 20);
INSERT INTO `role_menu` VALUES (2, 20);
INSERT INTO `role_menu` VALUES (1, 21);
INSERT INTO `role_menu` VALUES (2, 21);
INSERT INTO `role_menu` VALUES (1, 22);
INSERT INTO `role_menu` VALUES (1, 23);
INSERT INTO `role_menu` VALUES (1, 24);

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `cover` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg',
  `introduce` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `notice` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `user_id` int UNSIGNED NULL DEFAULT NULL,
  `rtmp_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `disabled` int NULL DEFAULT 0,
  `status` int NULL DEFAULT -1,
  `category_id` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `secret` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `play_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK_ROOM_CATEGORY_ID`(`category_id` ASC) USING BTREE,
  CONSTRAINT `FK_ROOM_CATEGORY_ID` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of room
-- ----------------------------
INSERT INTO `room` VALUES (1, '', 'Spring Security', 'https://images.unsplash.com/photo-1582917205301-bbb4afb5501f?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=1350&q=80', '简介', '通知公告', 10001, 'http://play.imhtb.cn/live/', 0, 1, 1, '2020-03-04 06:40:32', '2020-05-22 13:37:29', '1?lal_secret=5c1bf70154b2ed4c650573ff5be82c27', NULL);
INSERT INTO `room` VALUES (2, '', '官方直播间', 'https://images.unsplash.com/photo-1579599709180-375ce2a5db8b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=2125&q=80', '这里是官方直播间', '公告：24小时不间断直播', 10002, 'http://play.imhtb.cn/live/', 0, 1, 1, '2020-03-02 10:36:16', '2020-05-13 18:06:20', '2', NULL);
INSERT INTO `room` VALUES (5, NULL, '哈哈', 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/temp-15889987327273642532524691202410.jpg', NULL, '哈哈', 10007, NULL, 0, 0, 1, '2020-05-09 11:58:29', '2020-05-13 18:06:19', NULL, NULL);
INSERT INTO `room` VALUES (6, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10008, NULL, 0, -1, NULL, '2020-05-13 17:34:19', '2020-05-13 17:34:19', NULL, NULL);
INSERT INTO `room` VALUES (7, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10009, NULL, 0, 0, NULL, '2020-05-18 12:52:48', '2020-05-18 13:50:06', NULL, NULL);
INSERT INTO `room` VALUES (8, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10010, NULL, 0, -1, NULL, '2020-05-27 16:58:14', '2020-05-27 16:58:14', NULL, NULL);
INSERT INTO `room` VALUES (9, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10011, NULL, 0, -1, NULL, '2020-05-27 17:01:37', '2020-05-27 17:01:37', NULL, NULL);
INSERT INTO `room` VALUES (10, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10012, NULL, 0, -1, NULL, '2020-05-27 17:03:04', '2020-05-27 17:03:04', NULL, NULL);
INSERT INTO `room` VALUES (11, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10013, NULL, 0, -1, NULL, '2020-05-27 17:04:30', '2020-05-27 17:04:30', NULL, NULL);
INSERT INTO `room` VALUES (12, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10014, NULL, 0, -1, NULL, '2020-05-27 17:06:09', '2020-05-27 17:06:09', NULL, NULL);
INSERT INTO `room` VALUES (13, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10015, NULL, 0, -1, NULL, '2020-05-27 17:08:06', '2020-05-27 17:08:06', NULL, NULL);
INSERT INTO `room` VALUES (14, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10016, NULL, 0, -1, NULL, '2020-05-27 17:10:31', '2020-05-27 17:10:31', NULL, NULL);
INSERT INTO `room` VALUES (15, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10017, NULL, 0, -1, NULL, '2020-05-27 17:14:41', '2020-05-27 17:14:41', NULL, NULL);
INSERT INTO `room` VALUES (16, NULL, NULL, 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', NULL, NULL, 10018, NULL, 0, -1, NULL, '2020-05-27 17:57:45', '2020-05-27 17:57:45', NULL, NULL);
INSERT INTO `room` VALUES (17, NULL, 'cthy的直播间', 'https://ant-live-store-1253825991.cos.ap-chengdu.myqcloud.com/room-default-cover.jpeg', '这个主播还没有填写直播间简介', '欢迎来到直播间', 10019, NULL, 0, 1, NULL, '2026-04-24 16:03:31', '2026-04-26 08:32:47', '17?lal_secret=cb94a3736287b2947d296ad9b39fb682', NULL);

-- ----------------------------
-- Table structure for room_intimacy_rank
-- ----------------------------
DROP TABLE IF EXISTS `room_intimacy_rank`;
CREATE TABLE `room_intimacy_rank`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL,
  `user_id` int NOT NULL,
  `intimacy_value` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_room_user`(`room_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_room_intimacy_room`(`room_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '直播间亲密榜' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of room_intimacy_rank
-- ----------------------------
INSERT INTO `room_intimacy_rank` VALUES (1, 17, 10001, 13.00, '2026-04-24 23:20:37', '2026-04-26 08:34:42');
INSERT INTO `room_intimacy_rank` VALUES (3, 1, 10019, 220.00, '2026-04-25 19:41:54', '2026-04-26 08:39:13');

-- ----------------------------
-- Table structure for statistic_speak
-- ----------------------------
DROP TABLE IF EXISTS `statistic_speak`;
CREATE TABLE `statistic_speak`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `number` int NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of statistic_speak
-- ----------------------------
INSERT INTO `statistic_speak` VALUES (2, 1, NULL, 11, '2020-04-26', '2020-04-27 00:01:02', '2020-04-27 00:01:02');
INSERT INTO `statistic_speak` VALUES (3, 1, NULL, 33, '2020-04-26', '2020-04-27 00:07:00', '2020-04-27 00:07:00');
INSERT INTO `statistic_speak` VALUES (4, 1, NULL, 5, '2020-05-12', '2020-05-13 08:00:05', '2020-05-13 08:00:05');
INSERT INTO `statistic_speak` VALUES (5, 1, NULL, 1, '2020-05-17', '2020-05-18 08:00:05', '2020-05-18 08:00:05');
INSERT INTO `statistic_speak` VALUES (6, 1, NULL, 1, '2020-05-19', '2020-05-20 08:00:06', '2020-05-20 08:00:06');
INSERT INTO `statistic_speak` VALUES (7, 1, NULL, 2, '2020-05-20', '2020-05-21 08:00:05', '2020-05-21 08:00:05');

-- ----------------------------
-- Table structure for statistic_view
-- ----------------------------
DROP TABLE IF EXISTS `statistic_view`;
CREATE TABLE `statistic_view`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_id` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `member_number` int NULL DEFAULT NULL,
  `visitor_number` int NULL DEFAULT NULL COMMENT '游客浏览数',
  `total_number` int NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of statistic_view
-- ----------------------------
INSERT INTO `statistic_view` VALUES (1, 1, 1, 123, 23, 146, '2020-04-12', '', '2020-04-12 13:31:27', NULL);
INSERT INTO `statistic_view` VALUES (2, 1, 1, 23, 23, 46, '2020-04-08', '', '2020-04-12 13:31:27', NULL);
INSERT INTO `statistic_view` VALUES (3, 1, NULL, 0, 5, 5, '2020-04-26', NULL, '2020-04-27 00:07:00', '2020-04-27 00:07:00');
INSERT INTO `statistic_view` VALUES (4, 2, NULL, 0, 4, 4, '2020-05-05', NULL, '2020-05-06 00:00:08', '2020-05-06 00:00:08');
INSERT INTO `statistic_view` VALUES (5, 2, NULL, 0, 5, 5, '2020-05-09', NULL, '2020-05-10 08:00:05', '2020-05-10 08:00:05');
INSERT INTO `statistic_view` VALUES (6, 1, NULL, 0, 8, 5, '2020-05-10', NULL, '2020-05-11 08:00:05', '2020-05-11 08:00:05');
INSERT INTO `statistic_view` VALUES (7, 1, NULL, 0, 3, 3, '2020-05-11', NULL, '2020-05-12 08:00:05', '2020-05-12 08:00:05');
INSERT INTO `statistic_view` VALUES (8, 2, NULL, 0, 1, 1, '2020-05-11', NULL, '2020-05-12 08:00:05', '2020-05-12 08:00:05');
INSERT INTO `statistic_view` VALUES (9, 2, NULL, 0, 1, 1, '2020-05-12', NULL, '2020-05-13 08:00:05', '2020-05-13 08:00:05');
INSERT INTO `statistic_view` VALUES (10, 1, NULL, 0, 2, 2, '2020-05-12', NULL, '2020-05-13 08:00:05', '2020-05-13 08:00:05');
INSERT INTO `statistic_view` VALUES (11, 1, NULL, 0, 3, 3, '2020-05-13', NULL, '2020-05-14 08:00:05', '2020-05-14 08:00:05');
INSERT INTO `statistic_view` VALUES (12, 1, NULL, 0, 1, 1, '2020-05-17', NULL, '2020-05-18 08:00:05', '2020-05-18 08:00:05');
INSERT INTO `statistic_view` VALUES (13, 1, NULL, 0, 1, 1, '2020-05-18', NULL, '2020-05-19 08:00:05', '2020-05-19 08:00:05');
INSERT INTO `statistic_view` VALUES (14, 1, NULL, 0, 1, 1, '2020-05-19', NULL, '2020-05-20 08:00:06', '2020-05-20 08:00:06');
INSERT INTO `statistic_view` VALUES (15, 1, NULL, 0, 3, 3, '2020-05-20', NULL, '2020-05-21 08:00:05', '2020-05-21 08:00:05');
INSERT INTO `statistic_view` VALUES (16, 2, NULL, 0, 7, 7, '2020-05-21', NULL, '2020-05-22 08:00:05', '2020-05-22 08:00:05');
INSERT INTO `statistic_view` VALUES (17, 2, NULL, 0, 1, 1, '2020-05-22', NULL, '2020-05-23 08:00:05', '2020-05-23 08:00:05');

-- ----------------------------
-- Table structure for sys_push
-- ----------------------------
DROP TABLE IF EXISTS `sys_push`;
CREATE TABLE `sys_push`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `open` int NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `listener_items` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK_SYSTEM_PUSH_USER_ID`(`user_id` ASC) USING BTREE,
  CONSTRAINT `FK_SYSTEM_PUSH_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_push
-- ----------------------------
INSERT INTO `sys_push` VALUES (2, '794409767@qq.com', NULL, 10001, 1, '2020-05-08 18:02:13', '2020-05-27 18:13:00', 'salacity-notice');

-- ----------------------------
-- Table structure for sys_push_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_push_log`;
CREATE TABLE `sys_push_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `status` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `sys_push_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_push_log
-- ----------------------------
INSERT INTO `sys_push_log` VALUES (1, '房间ID：10001 色情检测置信度99', 1, '2020-05-15 16:14:44', '2020-05-20 16:14:42', 2);

-- ----------------------------
-- Table structure for tb_wallet
-- ----------------------------
DROP TABLE IF EXISTS `tb_wallet`;
CREATE TABLE `tb_wallet`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `balance` decimal(16, 2) NOT NULL DEFAULT 0.00,
  `version` int NOT NULL DEFAULT 0,
  `sign` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tb_wallet_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_wallet
-- ----------------------------
INSERT INTO `tb_wallet` VALUES (2, 10019, 448.00, 2, NULL, 0, '2026-04-25 18:18:17', '2026-04-26 08:39:13');
INSERT INTO `tb_wallet` VALUES (3, 10001, 210.00, 0, NULL, 0, '2026-04-25 19:26:57', '2026-04-26 08:39:13');

-- ----------------------------
-- Table structure for tb_wallet_log
-- ----------------------------
DROP TABLE IF EXISTS `tb_wallet_log`;
CREATE TABLE `tb_wallet_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `wallet_id` int NOT NULL,
  `balance` decimal(16, 2) NOT NULL,
  `fee` decimal(16, 2) NOT NULL,
  `action_type` int NOT NULL,
  `source_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tb_wallet_log_source_uuid`(`source_uuid` ASC) USING BTREE,
  INDEX `idx_tb_wallet_log_wallet_id`(`wallet_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_wallet_log
-- ----------------------------
INSERT INTO `tb_wallet_log` VALUES (1, 2, 648.00, 648.00, 1, 'ALR_10019_20260425184116_367275', 'alipay', '2026-04-25 19:41:36', '2026-04-25 19:41:36');
INSERT INTO `tb_wallet_log` VALUES (2, 2, 558.00, 10.00, 1, 'ALR_10019_20260425194458_244536', 'alipay', '2026-04-25 19:45:35', '2026-04-25 19:45:35');
INSERT INTO `tb_wallet_log` VALUES (3, 3, 100.00, -10.00, 2, NULL, 'gift_spend', '2026-04-26 08:34:42', '2026-04-26 08:34:42');
INSERT INTO `tb_wallet_log` VALUES (4, 2, 558.00, 10.00, 3, NULL, 'gift_income', '2026-04-26 08:34:42', '2026-04-26 08:34:42');
INSERT INTO `tb_wallet_log` VALUES (5, 2, 548.00, -10.00, 2, NULL, 'gift_spend', '2026-04-26 08:36:37', '2026-04-26 08:36:37');
INSERT INTO `tb_wallet_log` VALUES (6, 3, 110.00, 10.00, 3, NULL, 'gift_income', '2026-04-26 08:36:37', '2026-04-26 08:36:37');
INSERT INTO `tb_wallet_log` VALUES (7, 2, 448.00, -100.00, 2, NULL, 'gift_spend', '2026-04-26 08:39:14', '2026-04-26 08:39:14');
INSERT INTO `tb_wallet_log` VALUES (8, 3, 210.00, 100.00, 3, NULL, 'gift_income', '2026-04-26 08:39:14', '2026-04-26 08:39:14');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `signature` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `birthday` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `sex` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `nick_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `is_validated` int NULL DEFAULT 0,
  `disabled` int NULL DEFAULT 0,
  `role_id` int NOT NULL DEFAULT 100,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10020 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (10001, 'admin', '$2a$10$puULYxVheVu/sJZk7rUbvujNheV9v7afPWETHv47sjS2KAXNptTEe', NULL, 'http://image.imhtb.cn/avatar.png', '个性签名', NULL, '男', 'PinTeh', '2020-05-09 18:47:23', '2020-05-09 11:33:16', '794409767@qq.com', 0, 0, 1);
INSERT INTO `user` VALUES (10002, '10002', '$2a$10$GU9Ya.QrkZu.0TNxO4BuG.B9x26pD7Yl8jQUTENz3OuB3mBTqlWeC', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', '个性签名', NULL, '女', '官方直播账号', '2020-05-09 18:47:25', '2020-04-30 23:20:45', '3633530052@qq.com', 0, 0, 0);
INSERT INTO `user` VALUES (10007, '10007', '$2a$10$gZpGIsxQFU5tNCbLGr0uXuF3f7MYt2hKm66Mo532nblS.e6PX1KL.', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, 'Lequal', '2020-05-09 11:58:29', '2020-05-12 23:23:43', '2818028189@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10008, '10008', '$2a$10$VWEIsoOddZB4JVrVPMOnT.Kh29X1FYWl5TvRqIGXjyyWSNoJhwHPK', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, 'lequal', '2020-05-13 17:34:19', '2020-05-13 18:00:40', '1576070851@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10009, '10009', '$2a$10$M9M/hvCB42gZHqvbc2tzHOUZt1gVr0O5ZSsWyR/x4UpmzwsPxEbI.', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '测试用户1', '2020-05-18 12:52:48', '2020-05-19 11:43:40', '456456456@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10010, '10010', '$2a$10$ihwTAs8./uFG2prd7SDenOU1jhUe2N.2.VDP/SpgkyF33XUEeezQG', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 16:58:14', '2020-05-27 16:58:14', '36335300253@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10011, '10011', '$2a$10$sFn2iW4/L9fcGMKl.eqgM.tC1GcX.jRiKvOO4rFEAlAg4fqD50aNC', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 17:01:37', '2020-05-27 17:01:37', '3633532005@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10012, '10012', '$2a$10$uj9RWwancllT112ht0Uuo.q1Y/F2HpC52yVZVuU35Jh781GzvUh1G', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 17:03:04', '2020-05-27 17:03:04', '36332523005@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10013, '10013', '$2a$10$ETUeMp76vPNy9.I/.nUtiOAM5XuS8753VUvy1V723TEr2CsNOAxCS', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 17:04:30', '2020-05-27 17:04:30', '36335230055@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10014, '10014', '$2a$10$tiX1FP1Dd6CxlTYihhcrG.wxU5Mn2CtFMEMwEaMiFwsYSpqlnOEc2', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 17:06:09', '2020-05-27 17:06:09', '36366353005@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10015, '10015', '$2a$10$rVdkt24Vyu/DNfbnYrlwEOjffgThjqv4LIL0wNB.xelDm.BgbMkAa', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 17:08:06', '2020-05-27 17:08:06', '36343524345@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10016, '10016', '$2a$10$cAInYzageMiHtw4lHnIR8ejwhAa4/vRODPCot5kZ127Uk880qODiS', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '123123', '2020-05-27 17:10:31', '2020-05-27 17:10:31', '363355563005@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10017, '10017', '$2a$10$x3YS39ADdjwvEpbz6VwJQOn0SCCv3FTzvZPBjVRl8gQgzkRye90wG', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '12312', '2020-05-27 17:14:41', '2020-05-27 17:14:41', '363353005@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10018, '10018', '$2a$10$nYPILKoi2sI8ixY1A5a/KOQeCl4K6ggJ1yxGeprinoVRIBkw/FUFK', NULL, 'http://q1.qlogo.cn/g?b=qq&nk=363353005&s=100', NULL, NULL, NULL, '没有负担', '2020-05-27 17:57:45', '2020-05-27 17:58:09', '123123123@qq.com', 0, 0, 100);
INSERT INTO `user` VALUES (10019, 'cthy', '$2a$10$6U756VYtRfJW0mAFGnfqT.6R24BVWxQKBOZyuClTas3SjY4pw522a', '19391071473', 'http://localhost:9000/live.file.bucket/f823025f11a7436c961dc05e40527c40_1777040891.jpg', NULL, NULL, NULL, 'cthy', '2026-04-24 16:03:31', '2026-04-24 22:28:12', '19391071473@163.com', 0, 0, 100);

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
  INDEX `FK_USER_ROLE_ROLE_ID`(`role_id` ASC) USING BTREE,
  CONSTRAINT `FK_USER_ROLE_ROLE_ID` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_USER_ROLE_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_role
-- ----------------------------
INSERT INTO `user_role` VALUES (10001, 1, NULL, NULL);
INSERT INTO `user_role` VALUES (10002, 2, NULL, NULL);

-- ----------------------------
-- Table structure for video
-- ----------------------------
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `video_url` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `cover_url` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `file_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `video_category_id` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `status` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of video
-- ----------------------------
INSERT INTO `video` VALUES (1, 'http://1253825991.vod2.myqcloud.com/a62a57cfvodcq1253825991/d444c50c5285890802997421440/uCtVi1NBRxUA.mp4', 'https://images.unsplash.com/photo-1579599709180-375ce2a5db8b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=2125&q=80', '5285890802997421440', NULL, 10001, NULL, '2020-05-25 15:00:29', '2020-05-27 18:18:16', 'Spring Redis Startar', 0);
INSERT INTO `video` VALUES (2, 'http://1253825991.vod2.myqcloud.com/a62a57cfvodcq1253825991/8bd2d61b5285890803425878758/z1otW652Nw0A.mp4', 'https://images.unsplash.com/photo-1579599709180-375ce2a5db8b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=2125&q=80', '5285890803425878758', NULL, 10001, NULL, '2020-05-25 15:22:59', '2020-05-27 18:18:05', 'Spring Security', 0);

-- ----------------------------
-- Table structure for watch
-- ----------------------------
DROP TABLE IF EXISTS `watch`;
CREATE TABLE `watch`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `room_id` int NULL DEFAULT NULL,
  `watch_type` int NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_uid_rid_wt`(`user_id` ASC, `room_id` ASC, `watch_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of watch
-- ----------------------------
INSERT INTO `watch` VALUES (11, 10001, 2, 1, '2021-11-23 23:30:49', '2021-11-23 23:30:49');
INSERT INTO `watch` VALUES (12, 10001, 17, 0, '2026-04-24 16:05:33', '2026-04-24 16:05:33');
INSERT INTO `watch` VALUES (13, 10001, 17, 1, '2026-04-24 16:06:34', '2026-04-24 16:06:34');
INSERT INTO `watch` VALUES (14, 10019, 2, 0, '2026-04-24 22:45:34', '2026-04-24 22:45:34');
INSERT INTO `watch` VALUES (15, 10019, 1, 0, '2026-04-25 19:41:51', '2026-04-25 19:41:51');

-- ----------------------------
-- Table structure for withdrawal
-- ----------------------------
DROP TABLE IF EXISTS `withdrawal`;
CREATE TABLE `withdrawal`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `identity` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '收款账号',
  `identity_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '提现用户名',
  `mark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'alipay - wechat',
  `user_id` int NULL DEFAULT NULL COMMENT '用户id',
  `status` int NULL DEFAULT 0 COMMENT '当前状态 0-未完成 1-完成',
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `virtual_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '提现金豆',
  `real_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '提现金额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of withdrawal
-- ----------------------------
INSERT INTO `withdrawal` VALUES (1, '123', '123', 'mark', 'alipay', 10001, 0, '2020-04-09 15:53:12', '2020-04-09 15:53:14', 1000.00, 100.00);
INSERT INTO `withdrawal` VALUES (2, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', 'alipay', 10001, 1, '2020-04-09 23:30:46', NULL, 100.00, 10.00);
INSERT INTO `withdrawal` VALUES (3, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', 'alipay', 10001, 1, '2020-04-09 23:39:13', NULL, 100.00, 10.00);
INSERT INTO `withdrawal` VALUES (4, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', 'alipay', 10001, 1, '2020-04-10 00:12:12', NULL, 100.00, 10.00);
INSERT INTO `withdrawal` VALUES (5, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', 'alipay', 10001, 1, '2020-04-10 00:15:09', NULL, 100.00, 10.00);
INSERT INTO `withdrawal` VALUES (6, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', 'alipay', 10001, 1, '2020-04-10 00:20:50', NULL, 100.00, 10.00);
INSERT INTO `withdrawal` VALUES (7, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', '支付宝', 10001, 1, '2020-04-11 12:13:43', NULL, 10.00, 1.00);
INSERT INTO `withdrawal` VALUES (8, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', '支付宝', 10001, 1, '2020-05-11 23:54:40', '2020-05-11 23:54:40', 90.00, 18.00);
INSERT INTO `withdrawal` VALUES (9, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', '支付宝', 10001, 1, '2020-05-12 00:01:40', '2020-05-12 00:01:40', 100.00, 5.00);
INSERT INTO `withdrawal` VALUES (10, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', '支付宝', 10001, 1, '2020-05-12 00:24:03', '2020-05-12 00:24:03', 440.00, 22.00);
INSERT INTO `withdrawal` VALUES (11, 'ihydlk5321@sandbox.com', '沙箱环境', '提现', '支付宝', 10001, 1, '2020-05-20 16:07:35', '2020-05-20 16:07:35', 300.00, 15.00);

SET FOREIGN_KEY_CHECKS = 1;
