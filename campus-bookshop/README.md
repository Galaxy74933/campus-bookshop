# 网上购书系统（bookshop）

基于 **Java 21 + Spring Boot 3.3.5 + MyBatis + MySQL 8 + Thymeleaf** 的网上购书系统。

- **后台管理**（仅管理员）：创建图书（书名、作者、单价、库存）、图书分页查询（书名关键字）、编辑、删除。
- **前台书城**（仅普通读者）：浏览/搜索图书、加入购物车、修改数量、移除、清空购物车、结算支付、查看订单。
- **购书款**：读者注册成功系统自动赠送 **1000 元**购书款；结算时从余额扣款，余额不足或库存不足时整单失败并回滚。

## 配套文档（Word 版）

主文档：《网上购书系统技术文档及使用文档》（项目根目录，含使用文档与技术文档两部分）。

`docs/` 目录提供 8 份专题文档，与主文档配套使用：

| 文档 | 面向读者 | 内容 |
| --- | --- | --- |
| [网上购书系统-API接口文档](docs/网上购书系统-API接口文档.docx) | 联调与验收 | 全部路由、参数、校验规则、提示文案、curl 示例 |
| [网上购书系统-数据库设计文档](docs/网上购书系统-数据库设计文档.docx) | 开发与运维 | 表结构、ER 图、订单号规则、初始化数据 |
| [网上购书系统-部署运维手册](docs/网上购书系统-部署运维手册.docx) | 部署与运维 | 环境准备、构建启动、备份恢复、故障排除 |
| [网上购书系统-安全说明与改进方案](docs/网上购书系统-安全说明与改进方案.docx) | 开发与运维 | 安全现状、风险清单、未实施的改进建议 |
| [网上购书系统-开发指南与贡献指南](docs/网上购书系统-开发指南与贡献指南.docx) | 开发者 | 环境搭建、工程结构、核心机制、协作流程 |
| [网上购书系统-测试文档](docs/网上购书系统-测试文档.docx) | 测试与验收 | 自动化用例、人工验收用例、执行记录 |
| [网上购书系统-变更日志](docs/网上购书系统-变更日志.docx) | 全体 | 版本发布记录与升级指南 |
| [网上购书系统-服务条款与隐私政策](docs/网上购书系统-服务条款与隐私政策.docx) | 全体 | 使用规则与个人信息处理说明 |

> Word 文档中的目录页码为占位值，打开后请在目录区域右键选择「更新域」刷新。

## 演示账号（密码明文保存）

| 角色 | 用户名 | 密码 | 入口 |
| --- | --- | --- | --- |
| 管理员 | admin | admin123 | http://localhost:8080/admin/login http://localhost:8080/ |
| 读者 | reader | 123456 | http://localhost:8080/login（注册页可自助注册） |

## 快速开始

### 1. 准备数据库

本地 MySQL 8 启动后执行（有权限的账号即可）：

```sql
CREATE DATABASE IF NOT EXISTS bookshop_zcode DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 配置数据源

`src/main/resources/application.yml` 中按本机情况修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookshop_zcode?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
```

建表与初始数据（表结构 `db/schema.sql`、预置账号与示例图书 `db/data.sql`）由 Spring Boot `sql.init` 在启动时自动执行，脚本均为幂等写法，可重复启动。

### 3. 启动

```bash
mvn spring-boot:run
# 或
mvn -DskipTests package
java -jar target/bookshop-1.0.1.jar
```

默认端口 8080（被占用时可加 `--server.port=8081`）。

- 读者书城：<http://localhost:8080/> → 跳转登录
- 管理后台：<http://localhost:8080/admin/login>
- 接口调试：启动后访问 <http://localhost:8080/swagger-ui.html>

### 4. 运行测试

```bash
mvn test
```

`OrderServiceTest` 覆盖：正常结算（扣款+扣库存+清空购物车）、购书款不足回滚、库存不足、购物车为空、并发结算防超卖。

## 功能与业务规则

### 鉴权
- Session 登录 + `HandlerInterceptor` 角色拦截：`/admin/**` 仅 ADMIN，`/shop/**` 仅 READER；未登录/角色不符重定向到对应登录页。
- 两个登录入口互相隔离：管理员不能从读者入口登录，反之亦然。

### 读者注册与购书款
- 注册成功即写入 `balance = 1000.00`（`UserService.GIFT_AMOUNT`）。

### 购物车
- 重复加购自动累加数量；加购/改数量都校验「购物车数量 ≤ 当前库存」。
- 清空购物车一键删除当前用户全部购物车记录。

### 结算支付（`OrderService.checkout`，单事务）
1. 逐项条件扣库存：`UPDATE t_book SET stock = stock - ? WHERE id = ? AND stock >= ?`，影响行数为 0 即抛「库存不足」（并发安全，防超卖）；
2. 按数据库价格计算总额（不信任前端金额），条件扣款：`UPDATE t_user SET balance = balance - ? WHERE id = ? AND balance >= ?`，影响行数为 0 即抛「购书款不足」；
3. 写订单 + 明细（书名/单价快照）、清空购物车；
4. 任一步失败整体回滚：不扣款、不扣库存、购物车保留。

### 订单
- 支付成功生成订单号 `BSyyyyMMddHHmmss+随机数`，读者可在「我的订单」查看明细与金额。

## 工程结构

```
src/main/java/com/example/bookshop/
├── BookshopApplication.java
├── config/WebConfig.java              # 拦截器注册
├── config/OpenApiConfig.java          # Swagger 文档信息
├── interceptor/AuthInterceptor.java   # 登录/角色拦截 + 页面导航数据
├── controller/                        # Home / Auth / AdminBook / ShopBook / Cart / Order
├── service/                           # UserService / BookService / CartService / OrderService
├── mapper/                            # MyBatis 接口
├── entity/                            # User / Book / CartItem / Order / OrderItem
├── dto/                               # 表单与展示对象
└── exception/                         # BizException + 全局异常兜底
src/main/resources/
├── application.yml
├── db/schema.sql, db/data.sql         # 建表与初始化（幂等）
├── mapper/*.xml
├── static/css/style.css
└── templates/                         # Thymeleaf 页面（login/register/admin*/shop*）
src/test/java/com/example/bookshop/service/OrderServiceTest.java
```

## 数据库表

`t_user`（含 balance 购书款余额）、`t_book`、`t_cart_item`（唯一键 user_id+book_id）、`t_order`、`t_order_item`（下单快照）。详见 `src/main/resources/db/schema.sql` 与《数据库设计文档》。

> 说明：按需求约定，密码为明文保存与比对，仅适用于教学/演示场景。生产化改造建议见《安全说明与改进方案》。
