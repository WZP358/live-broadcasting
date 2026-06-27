CREATE TABLE IF NOT EXISTS `room_satisfaction` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `room_id` INT NOT NULL COMMENT '直播间ID',
  `user_id` INT NOT NULL COMMENT '评分用户ID',
  `score` TINYINT NOT NULL COMMENT '满意度评分，1到5星',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
  KEY `idx_room_score` (`room_id`, `score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间满意度评分';
