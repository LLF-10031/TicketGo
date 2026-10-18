-- TicketGo 初始化 DDL（MySQL 8，utf8mb4）
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS ticketgo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ticketgo;

-- 用户
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `password` VARCHAR(128) DEFAULT NULL COMMENT 'BCrypt密码',
  `nick_name` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 演出
CREATE TABLE IF NOT EXISTS `t_product` (
  `id` BIGINT NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `category` VARCHAR(50) DEFAULT NULL COMMENT '演唱会/话剧/体育/展览',
  `description` TEXT,
  `poster` VARCHAR(500) DEFAULT NULL COMMENT '海报URL',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已上架 2售罄 3已结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cat_status` (`category`,`status`),
  KEY `idx_status_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出';

-- 场次
CREATE TABLE IF NOT EXISTS `t_session` (
  `id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `venue` VARCHAR(200) DEFAULT NULL,
  `show_time` DATETIME DEFAULT NULL,
  `sale_start_time` DATETIME DEFAULT NULL,
  `sale_end_time` DATETIME DEFAULT NULL,
  `seat_mode` TINYINT DEFAULT 0 COMMENT '0免选座 1选座',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待开票 1售票中 2售罄 3已结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_prod_time` (`product_id`,`show_time`),
  KEY `idx_sale` (`sale_start_time`,`sale_end_time`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次';

-- 票档
CREATE TABLE IF NOT EXISTS `t_ticket_tier` (
  `id` BIGINT NOT NULL,
  `session_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `total_stock` INT NOT NULL DEFAULT 0,
  `sold_stock` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票档';

-- 座位
CREATE TABLE IF NOT EXISTS `t_seat` (
  `id` BIGINT NOT NULL,
  `session_id` BIGINT NOT NULL,
  `tier_id` BIGINT NOT NULL,
  `row_no` VARCHAR(10) DEFAULT NULL,
  `col_no` VARCHAR(10) DEFAULT NULL,
  `x` INT DEFAULT 0,
  `y` INT DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0可售 1锁定 2已售',
  `lock_token` VARCHAR(64) DEFAULT NULL,
  `lock_expire_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_tier_status` (`session_id`,`tier_id`,`status`),
  KEY `idx_lock_expire` (`lock_expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位';

-- 订单
CREATE TABLE IF NOT EXISTS `t_order` (
  `id` BIGINT NOT NULL,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `order_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1票订单 2券订单',
  `session_id` BIGINT DEFAULT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已退款 4已完成',
  `pay_expire_at` DATETIME DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `cancelled_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_time` (`user_id`,`status`,`create_time`),
  KEY `idx_status_expire` (`status`,`pay_expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 订单明细
CREATE TABLE IF NOT EXISTS `t_order_item` (
  `id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `tier_id` BIGINT DEFAULT NULL,
  `seat_id` BIGINT DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `quantity` INT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- 支付
CREATE TABLE IF NOT EXISTS `t_payment` (
  `id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `pay_type` VARCHAR(20) DEFAULT 'MOCK',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1成功 2失败 3已退款',
  `trade_no` VARCHAR(128) DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付';

-- 购物车
CREATE TABLE IF NOT EXISTS `t_cart` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `session_id` BIGINT NOT NULL,
  `tier_id` BIGINT NOT NULL,
  `seat_id` BIGINT DEFAULT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_session` (`user_id`,`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- 本地消息表
CREATE TABLE IF NOT EXISTS `t_message_outbox` (
  `id` BIGINT NOT NULL,
  `biz_key` VARCHAR(128) NOT NULL,
  `topic` VARCHAR(100) NOT NULL,
  `message_body` TEXT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2失败',
  `retry_count` INT NOT NULL DEFAULT 0,
  `max_retry` INT NOT NULL DEFAULT 5,
  `next_retry_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_key` (`biz_key`),
  KEY `idx_status_retry` (`status`,`next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';

-- 券模板
CREATE TABLE IF NOT EXISTS `t_coupon_template` (
  `id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `type` VARCHAR(20) NOT NULL COMMENT 'full_reduce/discount/immediate',
  `threshold_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `discount_amount` DECIMAL(10,2) DEFAULT 0,
  `discount_rate` DECIMAL(5,4) DEFAULT NULL,
  `max_discount` DECIMAL(10,2) DEFAULT NULL,
  `scope_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0全部 1指定演出',
  `scope_product_ids` VARCHAR(500) DEFAULT NULL,
  `valid_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0固定区间 1领取后N天',
  `valid_start` DATETIME DEFAULT NULL,
  `valid_end` DATETIME DEFAULT NULL,
  `valid_days` INT DEFAULT NULL,
  `stock_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0限量 1不限量',
  `total_qty` INT DEFAULT 0,
  `per_user_limit` INT NOT NULL DEFAULT 1,
  `acquire_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0领取 1购买',
  `price` DECIMAL(10,2) DEFAULT 0,
  `image_url` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已上架 2已结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`,`create_time`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

-- 用户券
CREATE TABLE IF NOT EXISTS `t_user_coupon` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `template_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未用 1锁定 2已用 3已释放 4已过期',
  `source_order_id` BIGINT DEFAULT NULL,
  `lock_order_id` BIGINT DEFAULT NULL,
  `received_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `expire_at` DATETIME DEFAULT NULL,
  `used_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_lock_order` (`lock_order_id`),
  KEY `idx_template_user` (`template_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户券';

-- 文件
CREATE TABLE IF NOT EXISTS `t_file` (
  `id` BIGINT NOT NULL,
  `biz_type` VARCHAR(20) NOT NULL COMMENT 'poster/avatar/seat_map/coupon',
  `biz_id` BIGINT DEFAULT NULL,
  `original_url` VARCHAR(500) DEFAULT NULL,
  `display_url` VARCHAR(500) DEFAULT NULL,
  `thumb_url` VARCHAR(500) DEFAULT NULL,
  `size` INT DEFAULT 0,
  `is_orphan` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`,`biz_id`),
  KEY `idx_orphan` (`is_orphan`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件';

-- 库存审计
CREATE TABLE IF NOT EXISTS `t_stock_audit` (
  `id` BIGINT NOT NULL,
  `target_type` VARCHAR(20) NOT NULL COMMENT 'tier/coupon_template',
  `target_id` BIGINT NOT NULL,
  `before_qty` INT DEFAULT 0,
  `after_qty` INT DEFAULT 0,
  `operator` VARCHAR(50) DEFAULT NULL,
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变更审计';

-- 通知（站内信）
CREATE TABLE IF NOT EXISTS `t_notification` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) DEFAULT 'SYSTEM',
  `title` VARCHAR(200) DEFAULT NULL,
  `content` VARCHAR(1000) DEFAULT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知/站内信';

-- 开票提醒
CREATE TABLE IF NOT EXISTS `t_reminder` (
  `id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `session_id` BIGINT NOT NULL,
  `remind_at` DATETIME NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待提醒 1已提醒 2已取消',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_remind` (`status`,`remind_at`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开票提醒';
