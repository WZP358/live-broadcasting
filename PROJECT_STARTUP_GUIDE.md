# 项目启动文档

## 1. 项目简介

本项目是一个本地可运行的直播平台 Demo，当前包含几条核心链路：

- 用户端首页、直播间、个人中心
- 主播端网页开播
- 观众端 WebRTC 优先观看，失败后回退到 HLS / FLV
- 聊天、礼物、亲密榜、浏览历史、关注等互动能力
- 后台管理：直播间、分类、消息、系统配置等
- 本地 `DeepFilterNet3` 降噪服务，供网页开播时调用

## 2. 目录结构

```text
d:\code\live
├─ backend                     Spring Boot 后端
├─ frontend                    Vue 3 + Vite 前端
├─ models\DeepFilterNet        DeepFilterNet3 模型与本地实时脚本
├─ tools\deepfilternet_denoise 本地 WebSocket 降噪服务
├─ start-maxine-denoise.cmd    降噪服务启动脚本（名字保留，实际已切到 DeepFilterNet3）
├─ start-local-live.cmd        历史本地 HLS 演示脚本（当前不作为主启动方式）
└─ stop-local-live.cmd         历史演示停止脚本
```

## 3. 环境要求

建议环境：

- Windows 10/11
- Java 8
- Maven 3.8+
- Node.js 18+
- MySQL 5.7 / 8.x
- Redis 6.x+
- Python 3.12

已知端口：

- `9000`：Spring Boot 后端
- `5174`：前端开发服务
- `18765`：本地 DeepFilterNet3 降噪 WebSocket 服务
- `10022`：房间聊天 / 互动 WebSocket
- `1935`：本地 RTMP 推流地址（配置中保留）
- `8080`：本地 HLS / FLV 拉流地址（配置中保留）

## 4. 启动前配置

### 4.1 后端配置

后端主配置文件：
[application.yml](d:/code/live/backend/src/main/resources/application.yml)

当前默认配置里最关键的是：

- MySQL：`jdbc:mysql://localhost:3306/ant-live`
- Redis：`localhost:6379`
- Spring Boot 端口：`9000`

启动前请至少确认：

1. 本地已创建数据库 `ant-live`
2. `application.yml` 里的数据库账号密码可用
3. Redis 已启动

### 4.2 前端配置

前端环境变量文件：

- [frontend/.env](d:/code/live/frontend/.env)
- [frontend/.env.dev](d:/code/live/frontend/.env.dev)

开发环境默认：

- 前端端口：`5174`
- API 前缀：`/api`
- 代理目标：`http://localhost:9000`

### 4.3 DeepFilterNet3 降噪配置

当前项目已经把网页端“实时降噪”切到 `DeepFilterNet3`。

相关文件：

- [start-maxine-denoise.cmd](d:/code/live/start-maxine-denoise.cmd)
- [server.py](d:/code/live/tools/deepfilternet_denoise/server.py)
- [realtime_df3.py](d:/code/live/models/DeepFilterNet/realtime_df3.py)

注意：

- `start-maxine-denoise.cmd` 这个名字是历史遗留
- 现在它启动的实际不是 Maxine，而是 `DeepFilterNet3`

## 5. 启动顺序

推荐按下面顺序启动。

### 第一步：启动 MySQL 和 Redis

请先确保：

- MySQL 正常运行
- Redis 正常运行

### 第二步：启动后端

在 `d:\code\live\backend` 下运行：

```powershell
mvn spring-boot:run
```

或者：

```powershell
mvn clean package
java -jar .\target\backend-1.0.0.RELEASE.jar
```

启动成功后，后端监听：

```text
http://localhost:9000
```

### 第三步：启动前端

在 `d:\code\live\frontend` 下运行：

```powershell
npm install
npm run dev
```

启动成功后访问：

```text
http://localhost:5174
```

### 第四步：启动本地降噪服务

在项目根目录 `d:\code\live` 下运行：

```powershell
.\start-maxine-denoise.cmd
```

实际启动的是：

```powershell
python D:\code\live\tools\deepfilternet_denoise\server.py
```

服务地址：

```text
ws://127.0.0.1:18765/ws
```

### 第五步：验证网页直播链路

验证顺序建议：

1. 打开前端首页
2. 登录账号
3. 进入“直播中心 / 开播设置”
4. 打开网页直播
5. 打开“实时降噪”
6. 用另一个页面进入直播间观看

## 6. 常用启动命令

### 后端

```powershell
cd D:\code\live\backend
mvn spring-boot:run
```

### 前端

```powershell
cd D:\code\live\frontend
npm install
npm run dev
```

### DeepFilterNet3 降噪服务

```powershell
cd D:\code\live
.\start-maxine-denoise.cmd
```

### DeepFilterNet3 本地实时桥接测试

仅用于本机音频设备测试：

```powershell
cd D:\code\live\models\DeepFilterNet
powershell -ExecutionPolicy Bypass -File .\run_realtime_df3.ps1 -ListDevices
```

## 7. 最小可运行链路

如果你只想先把“网页可打开”跑起来，最小需要：

1. MySQL
2. Redis
3. 后端 `9000`
4. 前端 `5174`

如果你还想用“实时降噪”，再额外启动：

5. `DeepFilterNet3` 本地降噪服务 `18765`

## 8. 故障排查

### 8.1 前端报 `ECONNREFUSED ::1:9000`

说明后端没启动，或者前端代理连不到后端。

先检查：

- `http://localhost:9000` 是否可访问
- 后端控制台是否启动成功

### 8.2 网页开播时“降噪服务不可用”

先检查：

```powershell
cd D:\code\live
.\start-maxine-denoise.cmd
```

如果服务没启动，前端就会回退到原始麦克风音频。

### 8.3 打开“实时降噪”后直播没声音

这个问题之前已经在前端音频图里修过。

相关文件：
[liveDenoise.js](d:/code/live/frontend/src/utils/liveDenoise.js)

如果再次遇到：

1. 重启前端 dev 服务
2. 刷新页面
3. 再重新启用“实时降噪”

### 8.4 前端 `vite build` 出现 `spawn EPERM`

这是本机环境里 `esbuild` 子进程权限问题，不一定是代码语法错误。

排查方向：

- 以普通终端而不是受限环境运行
- 检查杀毒软件 / 安全策略
- 删除 `node_modules` 后重装

### 8.5 DeepFilterNet3 实时处理卡顿

如果实时桥接日志里 `RTF > 1`，说明处理速度慢于实时。

可以调大块大小，例如：

```powershell
powershell -ExecutionPolicy Bypass -File .\run_realtime_df3.ps1 -InputDevice 12 -OutputDevice 10 -BlockSize 960 -ContextBlocks 12
```

## 9. 推荐开发顺序

如果你是第一次接这个项目，推荐顺序：

1. 先跑通后端和前端
2. 再验证用户端首页与直播间
3. 再测试网页开播
4. 最后再接入 DeepFilterNet3 降噪

## 10. 相关文档

项目讲解文档见：
[PROJECT_WALKTHROUGH.md](d:/code/live/PROJECT_WALKTHROUGH.md)
