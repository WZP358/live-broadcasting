CREATE TABLE IF NOT EXISTS `guardian_subscription` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '订阅者(粉丝)',
  `target_user_id` INT NOT NULL COMMENT '被订阅者(主播)',
  `level` TINYINT NOT NULL DEFAULT 1 COMMENT '守护等级 1青铜 2白银 3黄金',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '当月支付金额',
  `expire_time` DATETIME NOT NULL COMMENT '到期时间',
  `auto_renew` TINYINT DEFAULT 0 COMMENT '自动续费 0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '0已过期 1生效中',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_target` (`user_id`, `target_user_id`),
  INDEX `idx_target` (`target_user_id`, `status`),
  INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='守护订阅表';
