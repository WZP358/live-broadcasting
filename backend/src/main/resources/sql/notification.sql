CREATE TABLE IF NOT EXISTS `notification` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL COMMENT '接收用户ID',
    `type` VARCHAR(32) NOT NULL COMMENT '通知类型',
    `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(500) DEFAULT '' COMMENT '通知内容',
    `related_id` INT DEFAULT NULL COMMENT '关联ID',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_read` (`user_id`, `is_read`),
    INDEX `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';
