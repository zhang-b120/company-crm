#  CRM（finmaster）

公司内部 CRM 后端。当前仓库只有一个 Spring Boot 服务 `finmaster-server`，没有前端工程。

## 环境

| 项 | 版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.8+ |
| MySQL | 8.x，库名 `finmaster` |
| Spring Boot | 3.1.5 |
| MyBatis-Plus | 3.5.3 |

服务端口：`8090`。

## 启动

在 `finmaster-server` 目录：

```powershell
mvn spring-boot:run
```

或在 IDE 中运行 `com..finmaster.FinMasterApplication`。

启动前先准备 MySQL，并按下面「环境与密钥」填好本机配置。Flyway 已开启，脚本目录是 `classpath:db/migration`，基线版本 `2.0.18`。仓库里现有建表脚本在 `src/main/resources/script/ddl.sql`、`dml.sql`，不在 Flyway 目录下。

## 项目结构

源码目录是 `com/api/finmaster`，Java 包名是 `com..finmaster`。新建类时包名跟现有类保持一致。

```text
company-crm/
├── README.md
└── finmaster-server/
    ├── pom.xml
    └── src/main/
        ├── java/com/api/finmaster/     # 包名 com..finmaster
        │   ├── FinMasterApplication.java
        │   ├── analytics/              # 经营分析（线索、客户、订单、外勤、客服、OA）
        │   ├── chatbot/                # 通话质检对话（DeepSeek）
        │   ├── common/                 # 公共注解、工具、常量、统一返回
        │   ├── config/                 # Security、跨域、MyBatis、WebSocket、支付初始化
        │   ├── contract/               # 合同
        │   ├── cs/                     # 客服档案
        │   ├── customer/               # 客户、公司、线索
        │   ├── data/                   # 数据看板
        │   ├── history/                # 历史订单 / 公司服务
        │   ├── home/                   # 首页
        │   ├── invoice/                # 发票
        │   ├── knowledge/              # 知识库
        │   ├── oa/                     # 审批、考勤、请假、报销、薪酬、日程
        │   ├── order/                  # 订单
        │   ├── payment/                # 支付与微信支付配置（配置存在数据库）
        │   ├── registerAddress/        # 注册地址
        │   ├── sale/                   # 通话录音、外勤
        │   ├── scheduledTask/          # 定时任务
        │   ├── security/               # JWT、登录鉴权
        │   ├── system/                 # 用户、角色、菜单、部门、租户、字典、产品
        │   ├── task/                   # 任务
        │   ├── third/                  # 第三方：百度、腾讯云语音、企查查、天眼查、企业微信、DeepSeek
        │   ├── widget/                 # 小组件统计
        │   ├── workbench/              # 工作台
        │   ├── workflow/               # 流程
        │   └── workorder/              # 工单
        └── resources/
            ├── application.yml         # 提交到仓库的主配置
            ├── application-local.yml   # 本机密钥，已 gitignore，不要提交
            ├── config.properties       # RSA / MD5，当前随仓库提交
            ├── mapper/                 # MyBatis XML，按业务分子目录
            └── script/                 # ddl.sql、dml.sql
```

业务代码分层一般是 `controller` → `service` → `manager` → `mapper`。请求、响应放在对应模块的 `domain` 或 `entity` 下。

## 环境与密钥

配置分三处。改本机环境时，优先改 `application-local.yml`，不要把新的密钥写进会提交的文件。

`application.yml` 里有：

```yaml
spring:
  config:
    import: optional:classpath:application-local.yml
```

本机存在 `application-local.yml` 时会自动加载；文件不存在不影响启动。同名配置项以后加载的文件为准。

### 1. `application.yml`（会进 Git）

路径：`finmaster-server/src/main/resources/application.yml`

| 配置项 | 作用 |
| --- | --- |
| `server.port` | 服务端口，默认 `8090` |
| `spring.datasource.url` | MySQL 地址，库名 `finmaster` |
| `spring.datasource.username` | 数据库用户名 |
| `spring.datasource.password` | 数据库密码 |
| `third.deepseek.api-key` | DeepSeek Key，通话质检和 chatbot 使用 |
| `third.deepseek.api-url` | 默认 `https://api.deepseek.com/chat/completions` |
| `app.jwt.secret` | JWT 签名密钥 |
| `app.jwt.expiration` | Token 有效期，单位毫秒，默认 `86400000`（1 天） |

本机覆盖示例（写在 `application-local.yml`，不要提交）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/finmaster?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: your_user
    password: your_password

third:
  deepseek:
    api-key: your_deepseek_key

app:
  jwt:
    secret: your_jwt_secret
```

也可用环境变量，Spring Boot 会按键名宽松绑定，例如 `SPRING_DATASOURCE_PASSWORD`、`THIRD_DEEPSEEK_API_KEY`、`APP_JWT_SECRET`。

### 2. `application-local.yml`（不进 Git）

路径：`finmaster-server/src/main/resources/application-local.yml`  
忽略规则在 `finmaster-server/.gitignore`。

腾讯云录音识别只从这里或环境变量读取，代码里没有默认密钥。没配时，调用语音识别会直接报错。

```yaml
third:
  tencent:
    asr:
      secret-id: your_tencent_secret_id
      secret-key: your_tencent_secret_key
```

对应环境变量：`THIRD_TENCENT_ASR_SECRET_ID`、`THIRD_TENCENT_ASR_SECRET_KEY`。

### 3. 

| 用途 | 位置 |
| --- | --- |
| 百度 OCR：`APP_ID`、`API_KEY`、`SECRET_KEY` | `common/core/domain/constansts/BaiduConstants.java` |
| 企查查：`APP_KEY`、`APP_SECRET` | `common/core/domain/constansts/QccConstants.java` |
| 随联数聚身份证二要素：`MEMBER_ID`、`APP_KEY` | `common/core/domain/constansts/SlsjConstants.java` |
| 企业微信 corpId、应用 secret、会话存档密钥和私钥、回调 Token / AESKey | `common/core/domain/constansts/WxConstants.java` |
| 接口签名 RSA 私钥、公钥、MD5 key | `src/main/resources/config.properties` |
| 微信支付商户号、证书路径、APIv3 密钥 | 数据库支付配置表，由 `config/PaymentConfiguration.java` 启动时加载 |
| 上传文件磁盘路径 | `common/core/domain/constansts/FileConstants.java`，当前是 `E:\\file\...` |

文件域名在 `FileConstants.BASE_URL`。换机器部署时要同时改磁盘路径，并保证目录存在。


