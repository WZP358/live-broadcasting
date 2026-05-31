CREATE TABLE IF NOT EXISTS `notification_pref` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '用户ID',
  `live_start_enabled` TINYINT DEFAULT 1 COMMENT '开播提醒',
  `follow_enabled` TINYINT DEFAULT 1 COMMENT '关注提醒',
  `dnd_start` VARCHAR(5) DEFAULT NULL COMMENT '免打扰开始 HH:mm',
  `dnd_end` VARCHAR(5) DEFAULT NULL COMMENT '免打扰结束 HH:mm',
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知偏好表';
