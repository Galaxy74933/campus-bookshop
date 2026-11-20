-- 预置账号（密码明文保存）：管理员 admin/admin123，读者 reader/123456（已赠送 1000 元购书款）
INSERT IGNORE INTO t_user (username, password, role, balance) VALUES
('admin',  'admin123', 'ADMIN',  0.00),
('reader', '123456',   'READER', 1000.00);

-- 示例图书（已存在同名书则跳过）
INSERT INTO t_book (title, author, price, stock)
SELECT 'Java 核心技术（第12版）', 'Cay S. Horstmann', 149.00, 30
WHERE NOT EXISTS (SELECT 1 FROM t_book WHERE title = 'Java 核心技术（第12版）');

INSERT INTO t_book (title, author, price, stock)
SELECT '深入理解计算机系统', 'Randal E. Bryant', 139.00, 25
WHERE NOT EXISTS (SELECT 1 FROM t_book WHERE title = '深入理解计算机系统');

INSERT INTO t_book (title, author, price, stock)
SELECT '算法（第4版）', 'Robert Sedgewick', 99.00, 40
WHERE NOT EXISTS (SELECT 1 FROM t_book WHERE title = '算法（第4版）');

INSERT INTO t_book (title, author, price, stock)
SELECT 'Spring 实战（第6版）', 'Craig Walls', 89.00, 20
WHERE NOT EXISTS (SELECT 1 FROM t_book WHERE title = 'Spring 实战（第6版）');

INSERT INTO t_book (title, author, price, stock)
SELECT 'MySQL 必知必会', 'Ben Forta', 49.00, 50
WHERE NOT EXISTS (SELECT 1 FROM t_book WHERE title = 'MySQL 必知必会');
