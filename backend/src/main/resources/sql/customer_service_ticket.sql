CREATE TABLE IF NOT EXISTS `customer_service_ticket` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '提交用户ID',
  `category` VARCHAR(32) NOT NULL DEFAULT 'general' COMMENT '问题分类',
  `title` VARCHAR(120) NOT NULL COMMENT '问题标题',
  `content` VARCHAR(1000) NOT NULL COMMENT '问题描述',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待处理,1=已回复,2=已关闭',
  `handler_id` INT DEFAULT NULL COMMENT '处理管理员ID',
  `reply` VARCHAR(1000) DEFAULT '' COMMENT '客服回复',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reply_time` DATETIME DEFAULT NULL,
  INDEX `idx_customer_ticket_user` (`user_id`),
  INDEX `idx_customer_ticket_status` (`status`),
  INDEX `idx_customer_ticket_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单';
