CREATE TABLE IF NOT EXISTS `report` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `reporter_id` INT NOT NULL COMMENT '举报者ID',
  `target_user_id` INT DEFAULT NULL COMMENT '被举报用户ID',
  `room_id` INT DEFAULT NULL COMMENT '相关房间ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '举报类型: room/user/message',
  `target_id` VARCHAR(64) DEFAULT NULL COMMENT '被举报对象ID',
  `reason` VARCHAR(255) NOT NULL COMMENT '举报原因',
  `description` TEXT NULL COMMENT '补充说明/证据JSON',
  `status` TINYINT DEFAULT 0 COMMENT '0=待处理 1=已处理 2=已驳回',
  `handle_result` VARCHAR(500) DEFAULT '',
  `handler_id` INT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `handle_time` DATETIME DEFAULT NULL,
  INDEX `idx_reporter` (`reporter_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';
