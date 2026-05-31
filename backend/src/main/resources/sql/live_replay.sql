CREATE TABLE IF NOT EXISTS `live_replay` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `room_id` INT NOT NULL COMMENT '房间ID',
  `user_id` INT NOT NULL COMMENT '主播用户ID',
  `live_info_id` INT DEFAULT NULL COMMENT '关联直播记录ID',
  `title` VARCHAR(255) DEFAULT '' COMMENT '回放标题',
  `replay_url` VARCHAR(500) DEFAULT '' COMMENT '回放地址',
  `cover_url` VARCHAR(500) DEFAULT '' COMMENT '封面',
  `duration` BIGINT DEFAULT 0 COMMENT '时长(秒)',
  `status` TINYINT DEFAULT 0 COMMENT '0=录制中 1=已就绪 2=已删除',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
  `view_count` INT DEFAULT 0 COMMENT '播放次数',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_room_status` (`room_id`, `status`),
  INDEX `idx_live_info` (`live_info_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播回放表';
