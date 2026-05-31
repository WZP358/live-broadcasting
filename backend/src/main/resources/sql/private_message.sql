CREATE TABLE IF NOT EXISTS `private_message` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `from_user_id` INT NOT NULL,
  `to_user_id` INT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `is_read` TINYINT DEFAULT 0 COMMENT '0未读 1已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_conversation` (`from_user_id`, `to_user_id`),
  INDEX `idx_to_user` (`to_user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信表';
