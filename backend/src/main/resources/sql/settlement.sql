CREATE TABLE IF NOT EXISTS `settlement` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `period` VARCHAR(7) NOT NULL COMMENT '结算周期 YYYY-MM',
  `gift_income` DECIMAL(12,2) DEFAULT 0,
  `platform_fee` DECIMAL(12,2) DEFAULT 0 COMMENT '平台抽成',
  `net_income` DECIMAL(12,2) DEFAULT 0,
  `withdrawable` DECIMAL(12,2) DEFAULT 0,
  `withdrawn` DECIMAL(12,2) DEFAULT 0,
  `status` TINYINT DEFAULT 0 COMMENT '0待结算 1已结算 2已打款',
  `settle_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_period` (`user_id`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收益结算表';
