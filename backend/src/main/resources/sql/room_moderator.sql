CREATE TABLE IF NOT EXISTS `room_moderator` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `room_id` INT NOT NULL COMMENT '房间ID',
  `user_id` INT NOT NULL COMMENT '房管用户ID',
  `appointed_by` INT NOT NULL COMMENT '任命人(主播)ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
  INDEX `idx_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间管理员表';
