-- 网上购书系统表结构（幂等，可重复执行）

CREATE TABLE IF NOT EXISTS t_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)    NOT NULL COMMENT '用户名',
    password   VARCHAR(50)    NOT NULL COMMENT '密码（明文保存）',
    role       VARCHAR(20)    NOT NULL COMMENT '角色：ADMIN/READER',
    balance    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '购书款余额',
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS t_book (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200)   NOT NULL COMMENT '书名',
    author     VARCHAR(100)   NOT NULL DEFAULT '' COMMENT '作者',
    price      DECIMAL(10, 2) NOT NULL COMMENT '单价（元）',
    stock      INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_book_title (title)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书表';

CREATE TABLE IF NOT EXISTS t_cart_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL COMMENT '用户ID',
    book_id    BIGINT NOT NULL COMMENT '图书ID',
    quantity   INT    NOT NULL COMMENT '数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart_user_book (user_id, book_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车';

CREATE TABLE IF NOT EXISTS t_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(32)    NOT NULL COMMENT '订单号',
    user_id      BIGINT         NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

CREATE TABLE IF NOT EXISTS t_order_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL COMMENT '订单ID',
    book_id    BIGINT         NOT NULL COMMENT '图书ID',
    book_title VARCHAR(200)   NOT NULL COMMENT '书名快照',
    book_price DECIMAL(10, 2) NOT NULL COMMENT '单价快照',
    quantity   INT            NOT NULL COMMENT '购买数量',
    subtotal   DECIMAL(10, 2) NOT NULL COMMENT '小计',
    KEY idx_item_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';
