# PulseLive .env 配置说明

PulseLive 后端启动时会自动向上查找项目根目录的 `.env` 文件，并把里面的键值加入 Spring 配置。真实账号、密码、密钥只写在 `.env` 里，不要写回 `application.yml`。

## 快速开始

在项目根目录 `D:\code\live` 执行：

```powershell
Copy-Item .env.example .env
```

然后打开 `.env`，把空值或示例值替换成你自己的真实配置。改完 `.env` 后需要重启后端服务才会生效。

## 书写规则

每一行写一个配置：

```dotenv
KEY=value
```

常用规则：

- `#` 开头的是注释。
- `=` 左右不要加空格。
- 值里有空格时可以加双引号，例如 `ALIPAY_SUBJECT_PREFIX="PulseLive recharge"`。
- 密钥、私钥、数据库密码、短信平台密钥不要提交到 Git。
- 操作系统环境变量优先级高于 `.env`，`.env` 优先级高于 `application.yml` 里的默认值。

## 必填项

本地只跑基础功能时，通常先填这些：

```dotenv
DB_USERNAME=root
DB_PASSWORD=你的数据库密码
DB_URL=jdbc:mysql://localhost:3306/ant-live?characterEncoding=utf-8&serverTimezone=GMT%2b8
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
LAL_SECRET=本地直播推流密钥
```

如果要发邮件验证码，继续填写：

```dotenv
MAIL_USERNAME=你的邮箱
MAIL_PASSWORD=邮箱授权码
```

如果要启用云服务或支付，再按实际业务填写 Tencent、Aliyun、Alipay、MinIO 相关配置。

## 配置项索引

| 配置名 | 用途 |
| --- | --- |
| `SERVER_PORT` | 后端端口，默认 `9000` |
| `DB_USERNAME` / `DB_PASSWORD` / `DB_URL` | MySQL 连接信息 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接信息 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 邮件验证码发送账号和授权码 |
| `TENCENT_SMS_*` | 腾讯云短信配置 |
| `TENCENT_COS_*` | 腾讯云 COS 存储配置 |
| `ALIYUN_SMS_*` | 阿里云短信配置 |
| `ALIPAY_*` | 支付宝沙箱或正式环境配置 |
| `LAL_*` | 本地 LAL 直播服务密钥和推拉流地址 |
| `GUARD_*` | 直播安全检测服务开关、地址和检测间隔 |
| `MINIO_*` | MinIO 文件存储配置 |
| `PULSELIVE_*_SERVICE` | 后端启动时联动启动的本地服务名 |
| `PULSELIVE_LOCAL_LIVE_*_PORT` | 后端关闭时清理的本地直播端口 |
| `VERIFY_CODE_LOCAL_MAIL_FALLBACK` | 本地验证码兜底开关 |

完整模板见项目根目录的 `.env.example`。

## 注意事项

`.env` 已加入 `.gitignore`，但如果此前已经把真实密钥提交过，需要手动轮换这些密钥；仅删除代码里的明文并不能让旧提交记录里的密钥失效。
