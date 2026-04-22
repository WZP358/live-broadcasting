# 项目讲解文档

## 1. 项目定位

这是一个“直播平台全链路 Demo”。

它不是只有播放器，也不是只有后台，而是把下面这些能力串成了一条完整链路：

- 用户端：首页、直播间、个人中心
- 主播端：网页开播、直播设置、直播统计
- 互动能力：聊天、礼物、亲密榜、关注、浏览历史
- 平台治理：直播间管理、分类管理、消息管理、系统配置
- 音频增强：本地 `DeepFilterNet3` 实时降噪服务

换句话说，它已经具备“像一个小型直播平台一样工作”的基本骨架。

## 2. 技术栈

### 前端

- Vue 3
- Vite
- Ant Design Vue
- Pinia
- WebRTC / WebSocket
- HLS.js / FLV.js

前端目录：
[frontend](d:/code/live/frontend)

### 后端

- Spring Boot 2.5.8
- MyBatis Plus
- MySQL
- Redis
- WebSocket

后端目录：
[backend](d:/code/live/backend)

### 降噪能力

- DeepFilterNet3
- Python WebSocket 服务
- 浏览器 Web Audio 接入

相关目录：
[models/DeepFilterNet](d:/code/live/models/DeepFilterNet)
[tools/deepfilternet_denoise/server.py](d:/code/live/tools/deepfilternet_denoise/server.py)

## 3. 整体架构

可以把项目分成 5 层来看：

### 3.1 展示层

也就是用户能直接看到的页面：

- 首页
- 直播间
- 个人中心
- 后台管理页

核心文件：
[home.vue](d:/code/live/frontend/src/views/home.vue)
[room/index.vue](d:/code/live/frontend/src/views/room/index.vue)
[center/index.vue](d:/code/live/frontend/src/views/center/index.vue)
[system/index.vue](d:/code/live/frontend/src/views/system/index.vue)

### 3.2 业务接口层

前端通过 API 模块访问后端：

- 直播相关
- 房间相关
- 聊天相关

核心文件：
[live.js](d:/code/live/frontend/src/api/live.js)
[room.js](d:/code/live/frontend/src/api/room.js)
[chat.js](d:/code/live/frontend/src/api/chat.js)

### 3.3 实时互动层

这里处理：

- 观众进入房间
- 主播和观众之间的信令交互
- 聊天 WebSocket
- 直播观看回退

核心文件：
[browserLive.js](d:/code/live/frontend/src/utils/browserLive.js)
[BrowserLivePanel.vue](d:/code/live/frontend/src/views/center/live-settings/BrowserLivePanel.vue)
[Player.vue](d:/code/live/frontend/src/views/room/Player.vue)
[ChatList.vue](d:/code/live/frontend/src/views/room/ChatList.vue)

### 3.4 后端业务层

后端负责：

- 用户、房间、分类、消息等基础业务
- 鉴权
- 数据持久化
- 部分直播配置与平台管理能力

入口：
[AntLiveApplication.java](d:/code/live/backend/src/main/java/cn/imhtb/live/AntLiveApplication.java)

配置：
[application.yml](d:/code/live/backend/src/main/resources/application.yml)

### 3.5 音频增强层

网页开播时，浏览器会把麦克风音频交给本地 `DeepFilterNet3` 服务处理，再把增强后的音频替换回推流链路。

前端入口：
[liveDenoise.js](d:/code/live/frontend/src/utils/liveDenoise.js)

本地服务：
[server.py](d:/code/live/tools/deepfilternet_denoise/server.py)

模型与脚本：
[realtime_df3.py](d:/code/live/models/DeepFilterNet/realtime_df3.py)
[run_realtime_df3.ps1](d:/code/live/models/DeepFilterNet/run_realtime_df3.ps1)

## 4. 用户端怎么工作

## 4.1 首页

首页现在已经被改造成“内容平台风格”的直播发现页。

主要能力：

- 焦点推荐位
- 热门榜
- 分区导航
- 搜索和排序
- 最近观看
- 推荐直播流
- 分区楼层

核心文件：
[home.vue](d:/code/live/frontend/src/views/home.vue)
[LiveRoom.vue](d:/code/live/frontend/src/components/LiveRoom.vue)

### 4.2 直播间

直播间页现在分成几块：

- 顶部主播区
- 中间播放器
- 右侧互动区
- 底部房间信息和相关推荐

核心文件：
[room/index.vue](d:/code/live/frontend/src/views/room/index.vue)
[Player.vue](d:/code/live/frontend/src/views/room/Player.vue)
[ChatList.vue](d:/code/live/frontend/src/views/room/ChatList.vue)
[GiftList.vue](d:/code/live/frontend/src/views/room/GiftList.vue)

### 4.3 直播观看逻辑

观看优先级是：

1. 优先尝试网页直播低延迟链路
2. 如果浏览器直播不可用，则回退到 HLS / FLV 播放

关键文件：
[Player.vue](d:/code/live/frontend/src/views/room/Player.vue)

## 5. 主播端怎么工作

主播端主要在个人中心里的直播设置页面。

核心入口：
[live-settings/index.vue](d:/code/live/frontend/src/views/center/live-settings/index.vue)

真正控制网页开播的是：
[BrowserLivePanel.vue](d:/code/live/frontend/src/views/center/live-settings/BrowserLivePanel.vue)

它负责：

- 获取屏幕或摄像头流
- 获取麦克风
- 连接直播信令
- 创建 WebRTC 推流链路
- 连接字幕和降噪引擎
- 动态切换原始音频 / 降噪音频

## 6. DeepFilterNet3 降噪链路

这是这次改动里比较重要的一部分。

### 6.1 之前的问题

原来的网页降噪按钮绑定的是 `Maxine SDK` 本地服务。

所以当本地 Maxine 没启动时，前端会提示：

- “Maxine SDK 服务不可用”
- 然后回退原始音频

### 6.2 现在的方案

现在已经切换为：

- 前端仍然连接 `ws://127.0.0.1:18765/ws`
- 但服务端实现换成了 `DeepFilterNet3`

也就是说：

浏览器麦克风 -> Web Audio -> DeepFilterNet3 WebSocket -> 增强音频 -> 替换推流音轨

### 6.3 关键文件

前端：
[liveDenoise.js](d:/code/live/frontend/src/utils/liveDenoise.js)
[BrowserLivePanel.vue](d:/code/live/frontend/src/views/center/live-settings/BrowserLivePanel.vue)

服务端：
[server.py](d:/code/live/tools/deepfilternet_denoise/server.py)

模型侧：
[realtime_df3.py](d:/code/live/models/DeepFilterNet/realtime_df3.py)

### 6.4 注意点

- `start-maxine-denoise.cmd` 只是历史命名，实际已切到 `DeepFilterNet3`
- 实时性能取决于本机 CPU / GPU
- 如果处理速度跟不上，会出现延迟堆积

## 7. 后台部分怎么理解

后台的作用不是播放直播，而是“运营和治理”。

当前已经有这些模块：

- 控制台
- 用户管理
- 直播间管理
- 分类管理
- 消息管理
- 礼物管理
- 系统配置

它们的作用分别是：

- 直播间管理：改标题、分类、封禁状态
- 分类管理：控制前台频道导航
- 消息管理：回溯直播消息
- 礼物管理：控制前台礼物展示和价格
- 系统配置：平台基础参数

后台入口文件：
[router/index.js](d:/code/live/frontend/src/router/index.js)

代表性页面：
[system/dashboard/index.vue](d:/code/live/frontend/src/views/system/dashboard/index.vue)
[system/room/index.vue](d:/code/live/frontend/src/views/system/room/index.vue)
[system/category/index.vue](d:/code/live/frontend/src/views/system/category/index.vue)
[system/message/index.vue](d:/code/live/frontend/src/views/system/message/index.vue)

## 8. 这次前台改造做了什么

这轮用户端改造的目标不是单纯“换皮”，而是让它更像一个直播平台。

主要升级点：

- 首页改成了平台式内容流
- 卡片信息密度提升
- 直播间页增加了主播区、推荐区、说明区
- 增加了搜索、排序、分区楼层、继续观看、热门榜

核心改动文件：
[home.vue](d:/code/live/frontend/src/views/home.vue)
[LiveRoom.vue](d:/code/live/frontend/src/components/LiveRoom.vue)
[room/index.vue](d:/code/live/frontend/src/views/room/index.vue)

## 9. 推荐的继续完善方向

如果继续往“更像成熟平台”走，建议下一批做这些：

### 9.1 用户端

- 顶部全站导航和搜索建议
- 首页 Banner / 活动位
- 主播主页
- 直播间多 tab：简介 / 动态 / 排行 / 贡献榜

### 9.2 互动

- 弹幕开关
- 礼物连击
- 观众席和入场提示
- 房管 / 禁言 / 举报

### 9.3 平台运营

- 推荐位管理
- 直播审核状态
- 分区权重 / 热门排序策略
- 直播数据大盘

### 9.4 音视频

- 虚拟声卡接 OBS
- 音频设备选择界面
- 开播前设备自检
- 更完整的本地推流 / 回放测试工具

## 10. 你应该怎么读这个项目

如果你要快速理解项目，推荐阅读顺序：

1. 先看前端路由
[router/index.js](d:/code/live/frontend/src/router/index.js)

2. 再看首页和直播间
[home.vue](d:/code/live/frontend/src/views/home.vue)
[room/index.vue](d:/code/live/frontend/src/views/room/index.vue)

3. 再看主播开播页
[BrowserLivePanel.vue](d:/code/live/frontend/src/views/center/live-settings/BrowserLivePanel.vue)

4. 再看播放器和互动
[Player.vue](d:/code/live/frontend/src/views/room/Player.vue)
[ChatList.vue](d:/code/live/frontend/src/views/room/ChatList.vue)

5. 最后看降噪链路
[liveDenoise.js](d:/code/live/frontend/src/utils/liveDenoise.js)
[server.py](d:/code/live/tools/deepfilternet_denoise/server.py)

## 11. 相关文档

启动说明见：
[PROJECT_STARTUP_GUIDE.md](d:/code/live/PROJECT_STARTUP_GUIDE.md)
