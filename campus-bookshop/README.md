# 网上购书系统（bookshop）

基于 **Java 21 + Spring Boot 3.3.5 + MyBatis + MySQL 8 + Thymeleaf** 的网上购书系统。

- **后台管理**（仅管理员）：创建图书（书名、作者、单价、库存）、图书分类／搜索／分页查询、编辑、删除。
- **前台书城**（仅普通读者）：浏览/搜索图书、加入购物车、修改数量、移除、清空购物车、结算支付、查看订单。
- **购书款**：读者注册成功系统自动赠送 **1000 元**购书款；结算时从余额扣款，余额不足或库存不足时整单失败并回滚。

## 校园资源浏览与购书（2026-11-18）

支持图书、论文、期刊、会议论文、学位论文、报告、标准、其他资源。仅“图书”允许购买；其他资源可查看详情、加入学习书单，不参与购物车、支付或库存预警。

### 新增与编辑

- 所有资源必填：名称、资源类型、主分类。
- 仅图书必填：价格（大于 0，最多两位小数）、库存（非负整数）。其他类型不用填写，后台将销售字段归零。
- 选填：附加分类（可多选）、作者、作者单位、语言、来源／出版社、出版／发表日期、ISBN／DOI、内容简介。
- 一条资源有一个主分类和若干附加分类，按资源去重计数。分类被使用时禁止删除。
- 将图书改成非售卖类型会清除相关购物车条目，历史订单和书单保留。重新改为图书必须填写价格和库存。
- PDF 上传／在线阅读留待后续，本版展示资源元信息与简介，不提供附件功能。

### 多选检索

分类、资源类型、语言均可勾选多项，点击“应用所选条件”查询。按照用户要求，所有已选属性之间取并集：选择“计算机与信息”和“外文”会显示所有计算机资源以及所有外文资源，同一资源只出现一次。不选任何条件时显示全部。

搜索关键词在筛选结果中继续过滤。支持综合（名称、作者、来源及标识）、名称、作者、来源／出版社、ISBN／DOI（精确）搜索；不会搜索 PDF 正文。翻页、排序保留筛选条件。价格排序将可购买图书放在前面，其他资源置后。

### 数据与兼容

沿用现有数据源环境变量，无需新建数据库。启动执行 `schema.sql`、`catalog-schema.sql` 和原有 `data.sql`。

新增 `t_resource_info` 与 `t_resource_category`，保存资源元信息和附加分类；保留原有主分类表及图书、账户、余额、订单。已有图书默认仍为图书，未录入的语言显示“未填写”；旧书不会被猜测为中文。无类别的旧书仍为“待分类”，编辑时须选择主分类。旧专业课程表保留但不参与现有功能。

八个默认学科分类只初始化一次，不会在每次启动恢复用户已删除或改名的分类。数据库账户需要建表权限。

### 学习书单与库存

管理员维护公开书单，读者维护仅自己可见的个人书单；所有资源均可加入书单，同一本资源在同一书单不重复。

仅图书显示库存状态：0 本缺货、1～5 本偏低、超过 5 本正常；可通过 `bookshop.low-stock-threshold` 配置阈值。保留余额支付、库存回滚和历史订单快照。

### 本次新版本启动

在原 VS Code 运行终端按 Ctrl+C 停止旧程序，沿用原终端的数据库环境变量，再从项目目录运行：

```powershell
& 'D:\Desktop\LSH\Java_class\Programs\java21\bin\java.exe' -jar '.\target\resource-verification\bookshop-1.0.1.jar'
```

打开 http://localhost:8080/ 并刷新。不要误启上一轮 `catalog-verification` 下的旧 JAR。需要重建时：

```powershell
mvn '-Dbookshop.build.directory=target/resource-verification' package
```

### 验证

49 项自动测试通过：认证入口 25 项、分类／资源／书单 19 项、订单 5 项。使用隔离 H2，覆盖多选并集去重、分页保持筛选、元信息保存、条件必填、非售卖资源后端拦截、交易回滚、类型变更购物车清理和旧数据兼容。真实 MySQL 升级仍需用户重启后验收。

建议演示：管理员录入中文计算机图书与外文论文 → 读者勾选“计算机与信息＋外文”查看两者 → 点开论文详情（无购买按钮） → 将两者加入学习书单 → 购买图书并查看订单。

## 配套文档（Word 版）

主文档：《网上购书系统技术文档及使用文档》（项目根目录，含使用文档与技术文档两部分）。

`docs/` 目录提供 8 份专题文档，与主文档配套使用（老师原始参考文档，未同步本次分类和书单改造；当前功能以本 README 和源码为准）：

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

建表与初始数据（原表结构 `db/schema.sql`、分类及书单表 `db/catalog-schema.sql`、预置账号与示例图书 `db/data.sql`）由 Spring Boot `sql.init` 在启动时自动执行，脚本均为幂等写法，可重复启动。

### 3. 启动

```bash
mvn spring-boot:run
# 或
mvn -DskipTests package
java -jar target/bookshop-1.0.1.jar
```

默认端口 8080（被占用时可加 `--server.port=8081`）。

- 主界面：<http://localhost:8080/> → 选择管理员或读者入口
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

## 补充演示资源

原始 5 本图书位于 `src/main/resources/db/data.sql`。新增 12 条示例位于 `src/main/resources/db/demo-resources.json`（9 本图书、论文/期刊/报告各 1 条），均标明为虚构课程演示数据，无 PDF 附件。覆盖中文、外文、中外文对照及正常库存、低库存、缺货。

启动时先初始化分类，再由 `DemoResourceService` 在事务中导入这批数据。`demo-resources-v1` 标记保证只导入一次；已有同名资源会跳过，不修改原数据。后续编辑、改名或删除这批资源后，重启不会重新添加。若所需分类不存在，只在首次导入时补充。

本轮构建目录为 `target/sample-verification`；停止旧程序后使用该目录下的 `bookshop-1.0.1.jar` 启动。原来只有 5 条且无同名数据时，升级后共有 17 条资源，读者列表需翻页查看。

## 示例书单与详情排版

新增 5 个一次性示例书单（`db/demo-reading-lists.json`）：admin 的公开书单“Java Web 项目入门”“数学与工程基础”“学术阅读与研究方法”；reader 的个人书单“我的编程提升计划”“课外阅读与设计灵感”。其他读者只能看到 3 个公开书单。按资源名称关联已有内容，缺失的资源跳过；不会恢复被删除的资源。已有同名书单跳过，之后重启不会覆盖编辑或重新创建删除的书单。

资源详情的基本信息改为两列表格，标签使用浅色背景与行分隔线；简介使用独立区块，并保留 32px 上间距。本轮 JAR 位于 `target/reading-demo-verification/bookshop-1.0.1.jar`。
