CREATE TABLE IF NOT EXISTS `message` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `room_id` INT NOT NULL COMMENT '直播间ID',
  `from_uid` INT NOT NULL COMMENT '消息发送者用户ID',
  `content` TEXT COMMENT '消息内容',
  `reply_msg_id` INT DEFAULT NULL COMMENT '回复的消息ID',
  `status` TINYINT DEFAULT 0 COMMENT '消息状态 0正常 -1删除',
  `type` TINYINT DEFAULT 1 COMMENT '消息类型 1正常文本 2撤回消息',
  `extra` VARCHAR(1000) DEFAULT NULL COMMENT '扩展信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  INDEX `idx_room_id` (`room_id`),
  INDEX `idx_from_uid` (`from_uid`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播互动消息表';
