# Live Broadcasting

本项目是一个本地可运行的直播平台 Demo，包含 Vue 前端、Spring Boot 后端、本地直播媒体服务器、Redis/MySQL/MinIO，以及两个本地模型服务：实时降噪和直播内容审核。

## 项目结构

```text
.
├─ backend/                    Spring Boot 后端服务
│  ├─ src/main/java/           业务接口、直播、聊天、钱包、审核等后端代码
│  ├─ src/main/resources/      application.yml、application-local.yml、SQL、Mapper
│  └─ settings.xml             Maven 本地配置
├─ frontend/                   Vue 3 + Vite 前端
│  ├─ src/api/                 API 请求封装
│  ├─ src/views/               页面与业务视图
│  ├─ src/utils/               WebRTC、降噪、字幕等前端工具
│  └─ vite.config.js           本地开发代理配置
├─ models/
│  ├─ DeepFilterNet/           DeepFilterNet3 模型与代码
│  └─ live_check/              视觉审核模型服务，YOLO + NudeNet
├─ tools/
│  ├─ deepfilternet_denoise/   DeepFilterNet3 WebSocket 降噪服务
│  └─ local_live/              本地 RTMP / HTTP-FLV 直播媒体服务器
├─ start-local-live.cmd        启动本地直播媒体服务器
├─ start-maxine-denoise.cmd    启动 DeepFilterNet3 降噪服务
├─ start-live-guard.cmd        启动视觉审核模型服务
└─ stop-local-live.cmd         停止本地直播服务脚本
```

## 服务与端口

| 模块 | 服务 | 端口 / 地址 | 说明 |
|---|---|---:|---|
| 前端 | Vite dev server | `http://localhost:5173/` / `http://localhost:5174/` | `.env` 为 5173，`npm run dev -- --mode dev` 会读取 `.env.dev` 的 5174；端口被占用时 Vite 会自动顺延 |
| 后端 | Spring Boot API | `http://localhost:8088/` | 业务 API，前端 `/api` 代理到这里 |
| 后端 WebSocket | Netty 聊天 / WebRTC 信令 | `ws://localhost:10022/` | 聊天、礼物消息、网页直播信令 |
| 直播服务器 | RTMP 推流 | `rtmp://127.0.0.1:1935/live/` | 主播端推流入口 |
| 直播服务器 | HTTP-FLV 拉流 | `http://127.0.0.1:8080/live/{roomId}.flv` | 观众端直播拉流 |
| Redis | Redis | `localhost:6379` | 缓存、在线状态等 |
| MySQL | MySQL | `localhost:3306/ant-live` | 主业务数据库 |
| MinIO | MinIO | `http://localhost:9000` | 文件、封面、上传对象存储 |
| 模型服务 | DeepFilterNet3 降噪 | `ws://127.0.0.1:18765/ws` | 实时音频降噪 WebSocket |
| 模型服务 | Live Guard 视觉审核 | `http://127.0.0.1:8000/check` | 截图/直播内容审核接口 |
| 邮件 | QQ SMTP | `smtp.qq.com:465` | 验证码/邮件发送 |
| 阿里云短信 | 短信验证码 | `https://dypnsapi.aliyuncs.com` | 外部 HTTPS 服务，不占本地端口 |
| 支付宝沙箱 | 充值收银台 / 回调 | `https://openapi-sandbox.dl.alipaydev.com/gateway.do` | 外部 HTTPS 服务，不占本地端口 |

本地直播当前优先使用 HTTP-FLV，`backend/src/main/resources/application-local.yml` 中 `hlsPullStream` 保持为空，避免前端优先拿 `.m3u8`。

## 第三方服务

### 阿里云短信验证码

配置位置：`backend/src/main/resources/application.yml`

```yaml
aliyun:
  sms:
    enabled: true
    endpoint: dypnsapi.aliyuncs.com
    signName: 速通互联验证码
    templateCode: "100001"
    codeLength: 6
    validTimeSeconds: 300
```

用途：

- 注册、登录、绑定手机号等验证码发送。
- 后端通过 `AliyunSmsService` 调用阿里云短信接口。
- 这是外部 HTTPS 服务，默认走 `443`，本地不需要额外监听端口。

### 支付宝沙箱

配置位置：`backend/src/main/resources/application.yml`

```yaml
alipay:
  enabled: true
  verifySign: false
  gatewayUrl: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  returnUrl: http://localhost:5174/#/center/dollar/wallet
  notifyUrl: http://qbb9ec9c.natappfree.cc/api/v1/wallet/alipay/notify
  syncReturnUrl: http://qbb9ec9c.natappfree.cc/api/v1/wallet/alipay/return
```

用途：

- 钱包充值会创建支付宝沙箱订单，并打开沙箱收银台。
- 异步通知接口：`POST /api/v1/wallet/alipay/notify`
- 同步返回接口：`GET /api/v1/wallet/alipay/return`
- 支付宝沙箱是外部 HTTPS 服务，默认走 `443`，本地不监听独立端口。
- `notifyUrl` / `syncReturnUrl` 需要公网可访问地址，本地开发通常通过内网穿透映射到后端 `8088`。

### QQ 邮箱 SMTP

配置位置：`backend/src/main/resources/application.yml`

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    protocol: smtps
```

用途：

- 邮箱验证码、账号绑定通知等邮件发送。
- 使用 QQ 邮箱 SMTP 授权码登录，不是 QQ 密码。
- 这是外部 SMTPS 服务，连接 `smtp.qq.com:465`，本地不需要额外监听端口。

## 启动顺序

推荐先启动所有后端依赖，再启动前端页面。这样前端打开后，接口、直播、降噪、审核能力都已经可用。

1. 启动基础中间件：MySQL、Redis、MinIO。

2. 启动本地直播媒体服务器：

```powershell
.\start-local-live.cmd
```

3. 启动降噪模型服务：

```powershell
.\start-maxine-denoise.cmd
```

4. 启动视觉审核模型服务：

```powershell
.\start-live-guard.cmd
```

5. 启动后端：

```powershell
cd backend
mvn -s .\settings.xml spring-boot:run
```

6. 启动前端：

```powershell
cd frontend
npm run dev
```

注意：`npm run dev` 会使用 Vite 的 `dev` 模式，通常访问 `http://localhost:5174/`。如果你直接使用默认 `.env` 或已有端口占用，实际端口可能显示为 `5173` 或 Vite 自动顺延的新端口，以终端输出为准。

## 脚本命令速查

这些脚本都在项目根目录执行：

```powershell
# 启动本地直播媒体服务器，提供 RTMP 1935 和 HTTP-FLV 8080
.\start-local-live.cmd

# 停止本地直播媒体服务器，占用 1935 / 8080 的本地直播进程会被结束
.\stop-local-live.cmd

# 启动 DeepFilterNet3 实时降噪服务，监听 ws://127.0.0.1:18765/ws
.\start-maxine-denoise.cmd

# 启动 Live Guard 视觉审核服务，监听 http://127.0.0.1:8000/check
.\start-live-guard.cmd
```

如果希望手动进入目录启动，对应命令是：

```powershell
cd tools\local_live
npm start

cd ..\..
models\live_check\venv\Scripts\python.exe tools\deepfilternet_denoise\server.py

cd models\live_check
venv\Scripts\python.exe vision_guard.py
```

## 模型文件组织方式

两个模型的目录结构不一样，这是刻意保留的：

- `models/live_check/` 是一个自包含的视觉审核服务目录，服务代码、模型权重、依赖文件和 venv 都放在一起。
- `models/DeepFilterNet/` 保留的是 DeepFilterNet 上游项目结构，里面包含 Python 包、Rust 扩展、示例、模型权重等完整内容。
- `tools/deepfilternet_denoise/` 是本项目额外写的 WebSocket 桥接服务，用来把 DeepFilterNet3 接成前端可调用的实时降噪服务。

两个模型服务当前都优先使用同一个 Python 虚拟环境：

```text
models/live_check/venv/
```

### Live Guard 视觉审核模型

组织方式：服务目录内聚。

代码与模型文件：

```text
models/live_check/vision_guard.py
models/live_check/yolov8l.pt
models/live_check/yolov8l-pose.pt
```

依赖文件：

```text
models/live_check/requirements.txt
```

主要依赖：

- `torch`
- `opencv-python`
- `ultralytics`
- `nudenet`
- `fastapi`
- `uvicorn`
- `python-multipart`

安装命令：

```powershell
cd models\live_check
venv\Scripts\python.exe -m pip install -r requirements.txt
```

### DeepFilterNet3 降噪模型

组织方式：上游模型仓库和本项目服务入口分离。

服务入口：

```text
tools/deepfilternet_denoise/server.py
```

模型与 DeepFilterNet 源码：

```text
models/DeepFilterNet/DeepFilterNet/
models/DeepFilterNet/models/DeepFilterNet3/
```

依赖文件：

```text
tools/deepfilternet_denoise/requirements.txt
models/DeepFilterNet/DeepFilterNet/requirements.txt
models/DeepFilterNet/DeepFilterNet/pyproject.toml
```

本项目启动脚本会使用 `models/live_check/venv` 来跑降噪服务，因此降噪运行依赖也需要装进这个 venv：

```powershell
models\live_check\venv\Scripts\python.exe -m pip install -r tools\deepfilternet_denoise\requirements.txt
```

主要运行依赖：

- `torch`
- `torchaudio`
- `scipy`
- `websockets`
- `loguru`
- `deepfilterlib`
- `numpy`

维护时可以按下面的边界理解：

- 修改降噪 WebSocket 协议、端口、前端对接逻辑：优先改 `tools/deepfilternet_denoise/server.py`。
- 替换 DeepFilterNet3 权重：改 `models/DeepFilterNet/models/DeepFilterNet3/`。
- 调整 DeepFilterNet 模型内部代码：改 `models/DeepFilterNet/DeepFilterNet/`。
- 调整视觉审核检测逻辑：改 `models/live_check/vision_guard.py`。
- 替换视觉审核 YOLO 权重：改 `models/live_check/*.pt`。

## 关键配置

- 后端端口：`backend/src/main/resources/application.yml`
- 本地直播地址：`backend/src/main/resources/application-local.yml`
- 前端端口与代理：`frontend/.env`、`frontend/.env.dev`、`frontend/vite.config.js`
- 直播媒体服务器：`tools/local_live/server.js`
- 降噪服务：`tools/deepfilternet_denoise/server.py`
- 视觉审核服务：`models/live_check/vision_guard.py`

## 常用访问地址

- 前端页面：`http://localhost:5173/` 或 `http://localhost:5174/`，以 Vite 启动输出为准
- 后端 API：`http://localhost:8088/`
- MinIO：`http://localhost:9000`
- 视觉审核 Swagger：`http://127.0.0.1:8000/docs`
- 推流地址示例：`rtmp://127.0.0.1:1935/live/1`
- 拉流地址示例：`http://127.0.0.1:8080/live/1.flv`

## 备注

- 项目中的网页直播优先走 WebRTC，失败后回退到本地 FLV 拉流。
- 礼物打赏会扣送礼人钱包余额，并给主播钱包增加对应余额。
- `.runlogs/` 用于保存本地启动日志，不属于业务代码。
