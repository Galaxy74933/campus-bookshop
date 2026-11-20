# 校园学习书店

基于 **Java 21 + Spring Boot 3.3.5 + MyBatis + MySQL 8 + Thymeleaf** 的网上书店课程项目。在老师提供的 zcode 工程基础上，保留账户、购物车和余额支付功能，扩展校园资源检索、学习书单与库存预警。

系统采用 B/S 架构，由 Thymeleaf 在服务端渲染中文页面，无需单独启动前端工程。

源码位于 [`campus-bookshop/`](campus-bookshop/)。核心流程：**查找资源 → 查看详情 → 整理书单 → 加入购物车 → 余额支付 → 查看订单**。

## 功能概览

| 模块 | 管理员 | 读者 |
| --- | --- | --- |
| 注册与登录 | 独立入口、注册、登录、退出 | 独立入口、注册、登录、退出；注册赠送 1000 元购书款 |
| 资源 | 新增、编辑、删除、Excel 批量导入、检索及查看详情 | 检索、分页、排序及查看详情 |
| 分类 | 维护一级学科分类 | 按主分类或附加分类查找资源 |
| 学习书单 | 维护公开书单 | 查看公开书单，维护仅本人可见的个人书单 |
| 库存 | 查看库存状态，筛选低库存与缺货图书 | 查看可购数量，缺货图书不可购买 |
| 购买与订单 | 不参与读者购书流程 | 购物车、模拟余额支付、查看本人订单 |

支持图书、论文、期刊、会议论文、学位论文、报告、标准和其他资源。**只有图书可以购买**；其他资源可查看元信息和简介、加入学习书单，不参与支付或库存预警。

## Excel 导入与分页

资源列表右下角可选择每页 **10／20／50** 条，默认 10 条。切换后回到第一页并保留搜索、分类、类型、语言与排序条件。书单中的选书列表也支持此设置。

管理员进入 **资源管理 → Excel 导入**，下载空白模板或 20 条演示 Excel，选择文件后点击“开始导入”。模板和演示文件随项目一起提供，无需另装 Excel 才能上传。

- 仅支持 `.xlsx`，每次最多 500 行、5 MB；保留“资源导入”工作表名称、第一行表头和列顺序，从第二行填写。
- 所有资源必填名称、资源类型、主分类；图书还必填大于 0 的价格（最多两位小数）和非负整数库存。非图书不售卖，价格库存留空。
- 作者、语言、来源/出版社、日期、ISBN/DOI、作者单位、简介、附加分类选填。分类填写现有名称，多项附加分类用分号分隔；新分类须先创建。
- 日期支持 Excel 日期或 `yyyy-MM-dd` 文本。ISBN/DOI 建议按文本填写。不支持公式单元格。
- **整张表校验通过才写入**。错误显示 Excel 行号；任何行失败则整批不导入，数据库写入失败也会回滚。
- 相同 ISBN/DOI，或名称、作者、资源类型均相同，视为重复；会检查文件内和数据库已有资源，不覆盖已有数据。修改已有资源请使用编辑功能。
- 演示文件包含 16 本图书、2 篇论文、2 种期刊，均为虚构教学样例，包含外文、缺货、低库存及附加分类。成功导入一次即可，重复上传会提示重复。
- 每次导入会在数据库保存批次（文件名、时间、操作管理员、数量及资源编号），重新登录或重启后仍保留。资源管理页的 **撤回最近一次导入** 仅处理当前管理员自己的最新批次；确认后删除该批剩余资源，包括导入后编辑过的资源，清理购物车和书单关联，保留历史订单。已手动删除的自动跳过；重复撤回不影响更早批次，失败整批回滚。
- 此功能只记录更新版本后成功导入的数据，不能追溯旧版本的导入。撤回后可再次上传原 Excel 进行演示。
- 单条删除保留当前筛选、页码、每页数量与滚动位置；删空当前页时回到上一页（滚动恢复需要启用 JavaScript）。
- 下载路径：`/admin/books/import/template`、`/admin/books/import/demo`，均需管理员登录。

## AI 选书助手（读者自带 API）

读者登录后，在导航栏打开 **AI 选书助手**。输入学习目标、基础、语言等需求，一次生成最多 5 个站内资源推荐，显示推荐理由、阅读顺序、数据库价格和库存。可以查看详情、加入购物车或已有书单，也可以勾选结果保存为新的私人学习书单。论文等非图书资源不能购买。

### 使用步骤

1. 自行在 [DeepSeek 开放平台](https://platform.deepseek.com/)注册并开通 API，创建自己的 API 密钥。网页版聊天账号不等于 API 权限，费用以服务商说明为准。
2. 按本文启动方法，在自己的电脑运行本项目并登录读者账号。
3. 打开“AI 选书助手”，填写自己的 API 密钥，无需单独保存。页面不会回显密钥。
4. 输入需求，例如“学过一点 Java，想做网上书店项目，请推荐中文资源，偏向实践”。可单独填写总预算，点击“生成推荐”，一次提交密钥和需求。当前会话已有密钥时可以留空沿用，填写新密钥则更换。失败后保留需求和预算。
5. 查看推荐并核对识别条件，按需保存书单。使用完可点击“清除密钥与推荐记录”，或退出登录。

### 为什么项目作者不会得知你的私人 API 密钥？

**前提是：你下载源码，在你自己的电脑运行项目，而不是访问别人托管的网站。** 数据流如下：

```text
你的浏览器 → 你自己电脑上的 Spring Boot 后端 → DeepSeek 官方 API
```

GitHub 只提供源码下载。项目没有向作者发送密钥的接口或遥测逻辑，也没有作者提供的备用密钥。密钥仅随请求发往固定的 `https://api.deepseek.com/chat/completions`，不跟随 HTTP 重定向。项目作者不会因为你下载、运行本项目而收到你的密钥、需求或推荐结果。

- 密钥仅保存在当前读者登录会话的服务器内存中，不写入数据库、应用日志、源码、浏览器本地存储，也不回显到页面。不启用会话持久化。
- 退出登录、重新登录/切换账号、会话超时（默认闲置 30 分钟）、服务重启后须重新填写。清除密钥不会撤回已经发送给服务商的请求。
- 调用使用该读者的密钥，费用由其 DeepSeek API 账户承担，与本站模拟购书余额无关。
- 调用会将密钥（用于鉴权）、本次需求及候选资源元数据发送给 DeepSeek；不发送本站账号密码、余额、订单或私人书单。服务商如何处理这些数据，以其政策为准。
- **如果访问的是别人部署的网站，密钥会经过对方控制的服务器，服务器运行者在技术上可能读取它。** 本项目不能保证第三方托管方或修改版代码不会获取密钥。公开部署应使用 HTTPS，不要在不信任的网站填写密钥。
- 请勿将真实密钥写进 README、配置文件、测试、截图或 Git 提交；也不要启用会输出请求参数或 HTTP 鉴权头的调试日志。

### 推荐边界与失败处理

- 当前使用目录前 100 条资源的元数据作为候选，目录超过此规模时页面会提示限制。缺货图书不作为推荐；仅接受候选中的有效编号并去重、重新读取数据库信息。
- 总预算按推荐图书各一本合计，后端会剔除超预算项目。单独填写的金额优先于文字预算；未填写时由模型提取。语言约束由模型识别后校验，主题匹配仍依赖模型判断，不能保证理解始终正确。
- AI 只依据标题、作者、分类、语言、简介等元数据，未读取全文。部分演示资源为虚构数据，请勿将其推荐视为真实出版物评价。
- 推荐解释由 AI 生成，可能存在错误；书名、价格、库存由数据库展示。下单时仍按现有购物流程重新检查库存、价格与余额。
- 密钥无效、余额不足、限流或连接失败会给出提示并保留需求；不会改用作者或其他读者的密钥。未配置密钥不影响普通搜索、书单和购买功能。
- 第一版只支持 DeepSeek `deepseek-chat`，不做连续对话、不自动下单，也不将需求和推荐历史写入数据库。只有主动保存的书单会持久化。

## 环境要求

| 软件 | 版本或说明 |
| --- | --- |
| JDK | Java 21，包含 `java` 和 `javac` |
| Maven | 推荐 3.9.x，本项目未提供 Maven Wrapper |
| MySQL | 8.x，运行前需启动数据库服务 |
| 编辑器 | VS Code、IntelliJ IDEA 等均可 |

Spring Boot、Thymeleaf、MyBatis 和内嵌 Tomcat 由 Maven 自动下载，无需分别安装。数据库管理可使用 MySQL 命令行，Navicat 不是必需软件。

## 快速开始

### 1. 进入项目目录

克隆或下载仓库后，在仓库根目录执行：

```bash
cd campus-bookshop
java -version
javac -version
mvn -version
```

确认 Java 和 Maven 使用的 JDK 均为 21。后续命令均在包含 `pom.xml` 的 `campus-bookshop` 目录执行。

### 2. 创建数据库

先登录本机 MySQL：

```bash
mysql -u root -p
```

输入本机数据库密码，然后执行：

```sql
CREATE DATABASE IF NOT EXISTS bookshop_zcode
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

应用启动时会自动建表和初始化演示数据，无需手动逐个导入 SQL 文件。连接账户需要拥有该数据库的建表和读写权限。

### 3. 配置数据库连接

基础配置位于 [`application.yml`](campus-bookshop/src/main/resources/application.yml)，默认连接本机 `3306` 端口的 `bookshop_zcode` 数据库。推荐用环境变量覆盖本机用户名和密码，避免将真实凭据写入提交的配置文件。

在 **Windows PowerShell / VS Code PowerShell 终端**中执行：

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/bookshop_zcode?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
$dbCredential = Get-Credential -UserName 'root' -Message '输入本机 MySQL 账号和密码'
$env:SPRING_DATASOURCE_USERNAME = $dbCredential.UserName
$env:SPRING_DATASOURCE_PASSWORD = $dbCredential.GetNetworkCredential().Password
```

在 **macOS / Linux Bash 终端**中执行：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/bookshop_zcode?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='root'
read -r -s -p 'MySQL password: ' SPRING_DATASOURCE_PASSWORD
export SPRING_DATASOURCE_PASSWORD
```

环境变量只对当前终端及其启动的程序生效。请在**同一个终端**继续运行项目；新开终端后需要重新设置。

### 4. 启动应用

开发时直接启动：

```bash
mvn spring-boot:run
```

也可以先构建再运行 JAR：

```bash
mvn package
java -jar target/bookshop-1.0.1.jar
```

首次构建需要联网下载依赖。看到 `Started BookshopApplication` 后，打开 <http://localhost:8080/>。停止程序可在运行终端按 `Ctrl+C`。

端口被占用时，可指定新端口：

```bash
java -jar target/bookshop-1.0.1.jar --server.port=8081
```

此时访问 <http://localhost:8081/>。源码修改后需重新构建 JAR，或重新执行 `mvn spring-boot:run`。

### 在 VS Code 中运行

用 VS Code 打开 `campus-bookshop` 文件夹，在集成终端中完成上述数据库配置和启动步骤即可。也可安装 **Extension Pack for Java** 辅助编辑和调试；终端运行不依赖这些扩展。

## 演示账号与访问入口

以下账号在启动时初始化，仅用于本地演示：

| 角色 | 用户名 | 密码 | 登录页面 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | <http://localhost:8080/admin/login> |
| 读者 | `reader` | `123456` | <http://localhost:8080/login> |

- 主界面：<http://localhost:8080/>，选择角色入口后再选择登录、注册。
- 管理员注册：<http://localhost:8080/admin/register>。
- 读者注册：<http://localhost:8080/register>。
- Swagger 接口文档：<http://localhost:8080/swagger-ui.html>。

管理员与读者不能通过对方的登录入口登录。演示读者初始余额为 1000 元，支付后余额会保留，不会每次启动都重置。

## 业务规则

### 资源录入与分类

- 所有资源必填：名称、资源类型、主分类。
- 图书额外必填：价格大于 0，最多两位小数；库存为非负整数。
- 可选信息：作者、作者单位、语言、来源／出版社、出版／发表日期、ISBN／DOI、简介及附加分类。
- 一条资源有一个主分类和若干附加分类，被使用的分类不能删除。
- 将图书改为不可售卖类型时，会清理相关购物车条目，保留历史订单和书单；改回图书需重新填写价格、库存。

### 多选检索

学科分类、资源类型和语言均可多选，**所有已选条件之间取并集**。

例如选择“计算机与信息”和“外文”，将返回所有计算机资源和所有外文资源，同一资源只出现一次。关键词在该集合中继续过滤；未选择任何条件时显示全部资源。

搜索范围包括名称、作者、来源／出版社、ISBN／DOI 精确匹配，以及综合搜索。翻页和排序保留筛选条件；价格排序优先展示可售卖图书。本版检索元信息，不检索 PDF 正文。

### 学习书单与库存预警

管理员维护公开书单，读者维护仅本人可见的个人书单。所有资源均可加入书单，同一资源在同一书单中不能重复；个人书单不向其他读者或管理员开放。

图书库存为 0 时显示缺货，1～5 本为偏低，超过 5 本为正常。可通过 `bookshop.low-stock-threshold` 修改阈值，例如：

```bash
java -jar target/bookshop-1.0.1.jar --bookshop.low-stock-threshold=10
```

### 购物车与支付

重复加购会累加数量，加购和修改数量均检查当前库存。主动清空购物车不会扣款，也不会创建订单。

结算由 `OrderService.checkout` 在同一事务中完成：

1. 读取当前用户购物车，锁定并检查对应图书，按条件扣减库存。
2. 根据数据库中的单价计算金额，检查并扣减余额。
3. 生成订单与明细，保存成交时的书名、单价快照。
4. 清空购物车；任何一步失败均整体回滚，保留购物车。

支付为站内模拟购书款扣减，不对接真实支付平台。Session 和角色拦截器保护 `/admin/**` 与 `/shop/**` 的业务页面，角色入口、登录和注册页面公开可访问。

## 示例数据与初始化

| 数据 | 来源 | 说明 |
| --- | --- | --- |
| 基础账号与 5 本图书 | [`db/data.sql`](campus-bookshop/src/main/resources/db/data.sql) | 按用户名或书名检查是否存在后补充 |
| 12 条新增资源 | [`db/demo-resources.json`](campus-bookshop/src/main/resources/db/demo-resources.json) | 9 本图书，论文、期刊、报告各 1 条 |
| 5 个学习书单 | [`db/demo-reading-lists.json`](campus-bookshop/src/main/resources/db/demo-reading-lists.json) | 3 个公开书单、2 个个人书单 |

全新数据库首次启动后，正常情况下共 17 条资源。新增 12 条资源为虚构的课程演示数据，覆盖不同学科、语言和库存状态，不包含全文附件。

| 示例书单 | 创建账号 | 可见范围 |
| --- | --- | --- |
| Java Web 项目入门 | `admin` | 公开 |
| 数学与工程基础 | `admin` | 公开 |
| 学术阅读与研究方法 | `admin` | 公开 |
| 我的编程提升计划 | `reader` | 仅本人 |
| 课外阅读与设计灵感 | `reader` | 仅本人 |

默认分类、新增资源和示例书单使用 `t_app_migration` 记录初始化标记，只导入一次，后续编辑或删除不会在重启时自动恢复。书单按已有资源名称关联，缺失资源会跳过。基础 `data.sql` 则在每次启动时执行，删除或改名的原始示例图书可能被重新补充。

旧图书没有分类时显示“待分类”，语言未录入时显示“未填写”。历史专业、课程表不参与当前功能。

## 数据库设计

管理员与读者共用 `t_user`，通过 `role` 区分身份。

| 表 | 用途 |
| --- | --- |
| `t_user` | 账户、角色、购书款余额 |
| `t_book` | 资源基础信息及图书销售字段 |
| `t_resource_info` | 类型、语言、来源、日期、标识及简介 |
| `t_category` | 一级学科分类 |
| `t_book_category` | 资源的主分类 |
| `t_resource_category` | 资源的附加分类 |
| `t_reading_list` | 书单信息、所有者及公开状态 |
| `t_reading_list_book` | 书单与资源关联 |
| `t_cart_item` | 购物车条目 |
| `t_order`、`t_order_item` | 订单及成交明细快照 |
| `t_app_migration` | 一次性初始化标记 |

完整建表定义见 [`schema.sql`](campus-bookshop/src/main/resources/db/schema.sql) 和 [`catalog-schema.sql`](campus-bookshop/src/main/resources/db/catalog-schema.sql)。

## 工程结构

```text
campus-bookshop/
├── pom.xml
├── src/main/java/com/example/bookshop/
│   ├── BookshopApplication.java
│   ├── config/           # Web 配置与数据初始化
│   ├── controller/       # 页面路由与请求处理
│   ├── service/          # 业务校验、事务与权限逻辑
│   ├── mapper/           # MyBatis 数据访问接口
│   ├── entity/           # 实体
│   ├── dto/              # 表单与展示数据
│   ├── interceptor/      # Session 登录与角色检查
│   └── exception/        # 业务异常与统一处理
├── src/main/resources/
│   ├── application.yml
│   ├── db/               # 建表 SQL 与示例数据
│   ├── mapper/           # MyBatis SQL 映射
│   ├── static/           # CSS 与 JavaScript
│   └── templates/        # Thymeleaf 页面
└── src/test/             # 认证、资源、书单与订单测试
```

## 测试

```bash
mvn test
```

测试使用隔离的 H2 数据库，不需要启动本机 MySQL。AI 测试使用模拟响应，不需要真实密钥、不消耗 API 费用。当前测试集共 **73 项**：

| 测试类 | 数量 | 主要覆盖范围 |
| --- | --- | --- |
| `AuthPagesTest` | 25 | 角色入口、注册登录、访问控制、导航 |
| `CatalogFeatureTest` | 32 | 分类、资源校验、并集检索、书单权限、示例初始化、分页大小、Excel 导入与批次撤回、权限、关联清理、事务回滚、删除后位置和筛选保留 |
| `OrderServiceTest` | 5 | 正常结算、余额不足、库存不足、空购物车、并发扣库存 |
| `AiRecommendationControllerTest` | 5 | 读者权限、CSRF、密钥不回显、会话隔离与清除、结果转义、一次提交及失败保留需求 |
| `AiRecommendationServiceTest` | 6 | 候选编号校验、去重、预算与语言、错误响应、书单选择校验 |

测试报告生成于 `target/surefire-reports/`。H2 测试通过后，仍应在 MySQL 环境验证实际运行流程。

## 推荐演示流程

1. 从主界面进入管理员入口，登录并查看资源管理与库存筛选。
2. 查看资源录入表单的必填、选填字段，以及主分类和附加分类。
3. 退出后使用读者账号登录，勾选“计算机与信息＋外文”验证并集检索。
4. 查看图书与论文详情，比较购买入口和资源元信息。
5. 浏览公开书单和个人书单，从书单中选择图书加入购物车。
6. 修改数量、结算，检查余额、库存和订单明细。

## 当前范围与限制

- 已实现资源信息浏览、Excel 批量导入和模拟购书；未实现 PDF 上传、全文阅读、充值、物流和退款。
- 本项目用于课程学习与本地演示。当前密码明文保存，管理员注册入口公开，尚未具备生产环境所需的安全防护；公网部署前需要完善密码哈希、管理员创建权限、CSRF 防护等。
- 仓库收录源码与构建配置，构建产物、运行日志及本地课程参考文档不随源码提交。
