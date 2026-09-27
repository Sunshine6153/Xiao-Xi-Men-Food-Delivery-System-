-- 仅用于将旧版 orders / order_detail 迁移至当前结构；新建数据库请使用 create_tables.sql。
-- 执行前确认两张表没有需要保留的历史订单。

ALTER TABLE orders
  ADD COLUMN delivery_user_id BIGINT NULL AFTER user_id,
  MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN delivery_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER amount,
  ADD COLUMN order_delivery_time DATETIME NULL AFTER checkout_time,
  ADD COLUMN delivered_time DATETIME NULL AFTER order_delivery_time,
  ADD COLUMN tableware_amount INT NOT NULL DEFAULT 0 AFTER delivered_time,
  DROP COLUMN pay_method,
  DROP COLUMN pay_status,
  ADD INDEX idx_orders_status (status),
  ADD CONSTRAINT fk_orders_delivery_user FOREIGN KEY (delivery_user_id) REFERENCES user(id);

ALTER TABLE order_detail
  ADD COLUMN status TINYINT NOT NULL DEFAULT 1 AFTER image,
  ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time,
  ADD INDEX idx_order_detail_order_merchant (order_id, merchant_id);
