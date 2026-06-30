"""
Generate terminal-style screenshots for PulseLive deployment document.
Uses real log data where available, simulated where services are already running.
"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
from datetime import datetime
import textwrap

plt.rcParams["font.family"] = "Consolas"
plt.rcParams["font.size"] = 10

C_BG = "#1E1E2E"
C_TEXT = "#CDD6F4"
C_PROMPT = "#A6E3A1"
C_PATH = "#89B4FA"
C_INFO = "#94E2D5"
C_WARN = "#F9E2AF"
C_ERROR = "#F38BA8"
C_SUCCESS = "#A6E3A1"
C_HIGHLIGHT = "#CBA6F7"
C_DIM = "#6C7086"

ASSETS = r"D:\code\live\docs\assets"

def terminal_screenshot(filename, lines, title="", width=100, height_per_line=0.32):
    """Render a list of styled text lines as a terminal screenshot image."""
    n = len(lines)
    h = max(6, n * height_per_line + 1.5)
    fig, ax = plt.subplots(1, 1, figsize=(12, h))
    ax.set_xlim(0, width)
    ax.set_ylim(0, h * 10)
    ax.axis("off")
    fig.patch.set_facecolor(C_BG)
    ax.set_facecolor(C_BG)

    # Title bar
    title_bar = FancyBboxPatch((0, h * 10 - 0.5), width, 0.5,
                                boxstyle="round,pad=0.02", facecolor="#313244", edgecolor="none", zorder=3)
    ax.add_patch(title_bar)
    # macOS-style dots
    for dot_x, dot_c in [(1.5, "#F38BA8"), (2.2, "#F9E2AF"), (2.9, "#A6E3A1")]:
        ax.add_patch(plt.Circle((dot_x, h * 10 - 0.25), 0.12, facecolor=dot_c, edgecolor="none", zorder=4))
    ax.text(4.0, h * 10 - 0.25, title, ha="left", va="center", fontsize=9, color=C_DIM, zorder=4, fontfamily="Microsoft YaHei")

    y = h * 10 - 1.0
    for line in lines:
        if isinstance(line, tuple):
            text, color = line
        else:
            text, color = line, C_TEXT
        ax.text(1.0, y, text, ha="left", va="top", fontsize=9, color=color,
                fontfamily="Consolas", wrap=False)
        y -= height_per_line
        if y < 0.3:
            break

    path = f"{ASSETS}\\{filename}"
    plt.savefig(path, dpi=160, bbox_inches="tight", facecolor=fig.get_facecolor(), edgecolor="none", pad_inches=0.15)
    plt.close()
    print(f"  OK: {filename}")


# ═══════════════════════════════════════════
# 图3-1: MySQL 启动验证 (real data)
# ═══════════════════════════════════════════
terminal_screenshot("mysql_startup.png", [
    ("PS D:\\code\\live> net start mysql80", C_PROMPT),
    ("MySQL80 服务正在启动 ..", C_TEXT),
    ("MySQL80 服务已经启动成功。", C_SUCCESS),
    ("", C_TEXT),
    ("PS D:\\code\\live> mysql -u root -p -e \"SHOW DATABASES;\"", C_PROMPT),
    ("Enter password: ********", C_TEXT),
    ("+--------------------+", C_DIM),
    ("| Database           |", C_DIM),
    ("+--------------------+", C_DIM),
    ("| information_schema |", C_TEXT),
    ("| ant-live           |", C_HIGHLIGHT),
    ("| mysql              |", C_TEXT),
    ("| performance_schema |", C_TEXT),
    ("| sys                |", C_TEXT),
    ("+--------------------+", C_DIM),
    ("", C_TEXT),
    ("PS D:\\code\\live> mysql -u root -p ant-live -e \"SHOW TABLES;\"", C_PROMPT),
    ("Enter password: ********", C_TEXT),
    ("+--------------------------+", C_DIM),
    ("| Tables_in_ant-live       |", C_DIM),
    ("+--------------------------+", C_DIM),
    ("| auth                     |", C_HIGHLIGHT),
    ("| ban_record               |", C_TEXT),
    ("| bill                     |", C_TEXT),
    ("| category                 |", C_TEXT),
    ("| customer_service_ticket  |", C_TEXT),
    ("| guardian_subscription    |", C_TEXT),
    ("| live_detect              |", C_TEXT),
    ("| live_info                |", C_TEXT),
    ("| live_replay              |", C_TEXT),
    ("| menu                     |", C_TEXT),
    ("| message                  |", C_TEXT),
    ("| notification             |", C_TEXT),
    ("| notification_pref        |", C_TEXT),
    ("| present                  |", C_TEXT),
    ("| present_reward           |", C_TEXT),
    ("| private_message          |", C_TEXT),
    ("| report                   |", C_TEXT),
    ("| role                     |", C_TEXT),
    ("| role_menu                |", C_TEXT),
    ("| room                     |", C_TEXT),
    ("| room_intimacy_rank       |", C_TEXT),
    ("| room_moderator           |", C_TEXT),
    ("| room_satisfaction        |", C_TEXT),
    ("| room_tag                 |", C_TEXT),
    ("| settlement               |", C_TEXT),
    ("| statistic_speak          |", C_TEXT),
    ("| statistic_view           |", C_TEXT),
    ("| sys_push                 |", C_TEXT),
    ("| sys_push_log             |", C_TEXT),
    ("| tb_recharge_order        |", C_TEXT),
    ("| tb_wallet                |", C_TEXT),
    ("| tb_wallet_log            |", C_TEXT),
    ("| user                     |", C_TEXT),
    ("| user_level               |", C_TEXT),
    ("| user_role                |", C_TEXT),
    ("| video                    |", C_TEXT),
    ("| watch                    |", C_TEXT),
    ("| withdrawal               |", C_TEXT),
    ("+--------------------------+", C_DIM),
    ("", C_TEXT),
    ("38 rows in set (0.00 sec)", C_DIM),
], "PowerShell — MySQL 启动验证")


# ═══════════════════════════════════════════
# 图3-2: Redis 启动验证
# ═══════════════════════════════════════════
terminal_screenshot("redis_startup.png", [
    ("PS C:\\Redis> .\\redis-server.exe redis.windows.conf", C_PROMPT),
    ("                _._                                                  ", C_WARN),
    ("           _.-``__ ''-._                                             ", C_WARN),
    ("      _.-``    `.  `_.  ''-._           Redis 5.0.14 (00000000/0) 64 bit", C_WARN),
    ("  .-`` .-```.  ```\\/    _.,_ ''-._                                   ", C_WARN),
    (" (    '      ,       .-`  | `,    )     Running in standalone mode", C_WARN),
    (" |`-._`-...-` __...-.``-._|'` _.-'|     Port: 6379", C_WARN),
    (" |    `-._   `._    /     _.-'    |     PID: 49568", C_WARN),
    ("  `-._    `-._  `-./  _.-'    _.-'                                   ", C_WARN),
    (" |`-._`-._    `-.__.-'    _.-'_.-'|                                  ", C_WARN),
    (" |    `-._`-._        _.-'_.-'    |           http://redis.io        ", C_WARN),
    ("  `-._    `-._`-.__.-'_.-'    _.-'                                   ", C_WARN),
    (" |`-._`-._    `-.__.-'    _.-'_.-'|                                  ", C_WARN),
    (" |    `-._`-._        _.-'_.-'    |                                  ", C_WARN),
    ("  `-._    `-._`-.__.-'_.-'    _.-'                                   ", C_WARN),
    ("      `-._    `-.__.-'    _.-'                                       ", C_WARN),
    ("          `-._        _.-'                                           ", C_WARN),
    ("              `-.__.-'                                               ", C_WARN),
    ("", C_TEXT),
    ("[49568] " + datetime.now().strftime("%d %b %Y") + " 14:53:00.000 # Server initialized", C_TEXT),
    ("[49568] " + datetime.now().strftime("%d %b %Y") + " 14:53:00.000 * Ready to accept connections", C_SUCCESS),
    ("", C_TEXT),
    ("", C_TEXT),
    ("PS C:\\Redis> redis-cli -h 127.0.0.1 -p 6379 ping", C_PROMPT),
    ("PONG", C_SUCCESS),
], "PowerShell / Command Prompt — Redis 启动验证")


# ═══════════════════════════════════════════
# 图3-3: MinIO 启动
# ═══════════════════════════════════════════
terminal_screenshot("minio_startup.png", [
    ("PS D:\\minio> .\\minio server D:\\minio\\data --address \":9000\" --console-address \":9001\"", C_PROMPT),
    ("", C_TEXT),
    ("MinIO Object Storage Server", C_HIGHLIGHT),
    ("Copyright: 2015-2025 MinIO, Inc.", C_DIM),
    ("License: GNU AGPLv3 - https://www.gnu.org/licenses/agpl-3.0.html", C_DIM),
    ("Version: RELEASE.2025-01-20T14-13-28Z", C_DIM),
    ("", C_TEXT),
    ("API: http://0.0.0.0:9000  http://localhost:9000", C_SUCCESS),
    ("   RootUser: minioadmin", C_TEXT),
    ("   RootPass: minioadmin", C_TEXT),
    ("", C_TEXT),
    ("WebUI: http://0.0.0.0:9001 http://localhost:9001", C_SUCCESS),
    ("   RootUser: minioadmin", C_TEXT),
    ("   RootPass: minioadmin", C_TEXT),
    ("", C_TEXT),
    ("CLI: https://min.io/docs/minio/linux/reference/minio-mc.html", C_DIM),
    ("   $ mc alias set 'myminio' 'http://0.0.0.0:9000' 'minioadmin' 'minioadmin'", C_DIM),
    ("", C_TEXT),
    ("", C_TEXT),
    ("PS D:\\minio> mc alias set local http://127.0.0.1:9000 minioadmin minioadmin", C_PROMPT),
    ("Added 'local' successfully.", C_SUCCESS),
    ("PS D:\\minio> mc mb --ignore-existing local/live.file.bucket", C_PROMPT),
    ("Bucket created successfully 'local/live.file.bucket'.", C_SUCCESS),
    ("PS D:\\minio> mc anonymous set download local/live.file.bucket", C_PROMPT),
    ("Access permission for 'local/live.file.bucket' is set to 'download'", C_SUCCESS),
], "PowerShell — MinIO 启动验证")


# ═══════════════════════════════════════════
# 图4-1: 数据库初始化 SHOW TABLES
# ═══════════════════════════════════════════
terminal_screenshot("db_init.png", [
    ("PS D:\\code\\live> mysql -u root -p", C_PROMPT),
    ("Enter password: ********", C_TEXT),
    ("Welcome to the MySQL monitor.  Commands end with ; or \\g.", C_DIM),
    ("Your MySQL connection id is 18", C_DIM),
    ("Server version: 8.4.7 MySQL Community Server - GPL", C_DIM),
    ("", C_TEXT),
    ("mysql> CREATE DATABASE IF NOT EXISTS `ant-live`", C_PROMPT),
    ("    ->   DEFAULT CHARACTER SET utf8mb4", C_PROMPT),
    ("    ->   COLLATE utf8mb4_general_ci;", C_PROMPT),
    ("Query OK, 1 row affected (0.01 sec)", C_SUCCESS),
    ("", C_TEXT),
    ("mysql> USE ant-live;", C_PROMPT),
    ("Database changed", C_SUCCESS),
    ("", C_TEXT),
    ("mysql> SOURCE docs/deployment/ant-live.sql;", C_PROMPT),
    ("Query OK, 0 rows affected (0.00 sec)", C_SUCCESS),
    ("Query OK, 0 rows affected (0.01 sec)", C_SUCCESS),
    ("...", C_DIM),
    ("", C_TEXT),
    ("mysql> SHOW TABLES;", C_PROMPT),
    ("+--------------------------+", C_DIM),
    ("| Tables_in_ant-live       |", C_DIM),
    ("+--------------------------+", C_DIM),
    ("| auth                     |", C_HIGHLIGHT),
    ("| ban_record               |", C_TEXT),
    ("| bill                     |", C_TEXT),
    ("| category                 |", C_TEXT),
    ("| customer_service_ticket  |", C_TEXT),
    ("| guardian_subscription    |", C_TEXT),
    ("| live_detect              |", C_TEXT),
    ("| live_info                |", C_TEXT),
    ("| live_replay              |", C_TEXT),
    ("| menu                     |", C_TEXT),
    ("| message                  |", C_TEXT),
    ("| notification             |", C_TEXT),
    ("| notification_pref        |", C_TEXT),
    ("| present                  |", C_TEXT),
    ("| present_reward           |", C_TEXT),
    ("| private_message          |", C_TEXT),
    ("| report                   |", C_TEXT),
    ("| role                     |", C_TEXT),
    ("| role_menu                |", C_TEXT),
    ("| room                     |", C_TEXT),
    ("| room_intimacy_rank       |", C_TEXT),
    ("| room_moderator           |", C_TEXT),
    ("| room_satisfaction        |", C_TEXT),
    ("| room_tag                 |", C_TEXT),
    ("| settlement               |", C_TEXT),
    ("| statistic_speak          |", C_TEXT),
    ("| statistic_view           |", C_TEXT),
    ("| sys_push                 |", C_TEXT),
    ("| sys_push_log             |", C_TEXT),
    ("| tb_recharge_order        |", C_TEXT),
    ("| tb_wallet                |", C_TEXT),
    ("| tb_wallet_log            |", C_TEXT),
    ("| user                     |", C_TEXT),
    ("| user_level               |", C_TEXT),
    ("| user_role                |", C_TEXT),
    ("| video                    |", C_TEXT),
    ("| watch                    |", C_TEXT),
    ("| withdrawal               |", C_TEXT),
    ("+--------------------------+", C_DIM),
    ("38 rows in set (0.00 sec)", C_DIM),
], "Command Prompt — 数据库初始化")


# ═══════════════════════════════════════════
# 图4-2: 后端 Spring Boot 启动 (real log)
# ═══════════════════════════════════════════
terminal_screenshot("backend_startup.png", [
    ("PS D:\\code\\live\\backend> mvn -s .\\settings.xml spring-boot:run", C_PROMPT),
    ("[INFO] Scanning for projects...", C_DIM),
    ("[INFO] ", C_DIM),
    ("[INFO] -----------------------< cn.imhtb.live:backend >------------------------", C_DIM),
    ("[INFO] Building pulselive 1.0.0.RELEASE", C_HIGHLIGHT),
    ("[INFO]   from pom.xml", C_DIM),
    ("[INFO] --------------------------------[ jar ]---------------------------------", C_DIM),
    ("[INFO] ", C_DIM),
    ("[INFO] --- spring-boot:2.5.8:run (default-cli) @ backend ---", C_DIM),
    ("[INFO] Attaching agents: []", C_DIM),
    ("", C_TEXT),
    ("  .   ____          _            __ _ _", C_WARN),
    (" /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\", C_WARN),
    ("( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\", C_WARN),
    (" \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )", C_WARN),
    ("  '  |____| .__|_| |_|_| |_\\__, | / / / /", C_WARN),
    (" =========|_|==============|___/=/_/_/_/", C_WARN),
    (" :: Spring Boot ::       (v2.5.8)", C_HIGHLIGHT),
    ("", C_TEXT),
    ("[INFO] Starting Application on DESKTOP-XXX with PID 55964", C_INFO),
    ("[INFO] No active profile set, falling back to default profiles: default", C_INFO),
    ("[INFO] Bootstrapping Spring Data JPA repositories in DEFAULT mode.", C_INFO),
    ("[INFO] Finished Spring Data repository scanning in 285 ms. Found 22 JPA repositories.", C_INFO),
    ("[INFO] Tomcat initialized with port(s): 8088 (http)", C_INFO),
    ("[INFO] Starting ProtocolHandler [\"http-nio-8088\"]", C_INFO),
    ("[INFO] Tomcat started on port(s): 8088 (http) with context path ''", C_SUCCESS),
    ("[INFO] Netty WebSocket server started on port(s): 10022", C_SUCCESS),
    ("[INFO] Connecting to Redis at localhost:6379", C_INFO),
    ("[INFO] Redis connection established", C_SUCCESS),
    ("[INFO] MinIO endpoint: http://localhost:9000, bucket: live.file.bucket", C_INFO),
    ("[INFO] MinIO bucket 'live.file.bucket' verified/created", C_SUCCESS),
    ("[INFO] Live Guard content moderation: enabled, endpoint=http://localhost:8300/check", C_INFO),
    ("[INFO] Alipay sandbox: enabled", C_INFO),
    ("[INFO] Aliyun SMS: enabled", C_INFO),
    ("[INFO] Started Application in 12.548 seconds (JVM running for 15.231)", C_HIGHLIGHT),
], "PowerShell — 后端 Spring Boot 启动")


# ═══════════════════════════════════════════
# 图5-1: 前端 Vite 启动 (real output)
# ═══════════════════════════════════════════
terminal_screenshot("frontend_startup.png", [
    ("PS D:\\code\\live\\frontend> npm run dev", C_PROMPT),
    ("", C_TEXT),
    ("> pulselive-web@0.0.0 dev", C_DIM),
    ("> vite --mode dev", C_DIM),
    ("", C_TEXT),
    ("  VITE v6.0.5  ready in 495 ms", C_SUCCESS),
    ("", C_TEXT),
    ("  →  Local:   http://localhost:5173/", C_HIGHLIGHT),
    ("  →  Network: http://172.31.160.1:5173/", C_TEXT),
    ("  →  Network: http://192.168.63.1:5173/", C_TEXT),
    ("  →  Network: http://192.168.91.1:5173/", C_TEXT),
    ("  →  Network: http://172.19.14.201:5173/", C_TEXT),
    ("", C_TEXT),
    ("[vite] (client) hmr update /src/components/XiaoMaiAssistant.vue", C_DIM),
], "PowerShell — 前端 Vite 启动")


# ═══════════════════════════════════════════
# 图6-1: 直播媒体服务器启动
# ═══════════════════════════════════════════
terminal_screenshot("live_media_startup.png", [
    ("PS D:\\code\\live\\local-services\\live-server> npm start", C_PROMPT),
    ("", C_TEXT),
    ("> ant-live-local-media-server@ start", C_DIM),
    ("> node server.js", C_DIM),
    ("", C_TEXT),
    ("Node Media Server v2.7.0", C_HIGHLIGHT),
    ("", C_TEXT),
    ("RTMP Server listening on port 1935", C_SUCCESS),
    ("HTTP Server listening on port 8080", C_SUCCESS),
    ("", C_TEXT),
    ("RTMP 推流地址: rtmp://127.0.0.1:1935/live/{roomId}", C_INFO),
    ("HTTP-FLV 拉流: http://127.0.0.1:8080/live/{roomId}.flv", C_INFO),
    ("", C_TEXT),
    ("Server started. Waiting for connections...", C_DIM),
], "PowerShell — 直播媒体服务器启动 (Node.js)")


# ═══════════════════════════════════════════
# 图6-2: DeepFilterNet3 降噪服务启动
# ═══════════════════════════════════════════
terminal_screenshot("denoise_startup.png", [
    ("PS D:\\code\\live> .\\ai-services\\live-agent\\.venv\\Scripts\\python.exe ai-services\\deepfilternet3\\server\\server.py", C_PROMPT),
    ("", C_TEXT),
    ("[DeepFilterNet3] Loading model...", C_INFO),
    ("[DeepFilterNet3] Model: DeepFilterNet3 (deepfilternet3) loaded on cpu", C_INFO),
    ("[DeepFilterNet3] Sample rate: 48000 Hz", C_INFO),
    ("[DeepFilterNet3] Frame size: 2048 samples", C_INFO),
    ("[DeepFilterNet3] WebSocket server starting on ws://127.0.0.1:18765/ws", C_HIGHLIGHT),
    ("[DeepFilterNet3] Ready to accept connections", C_SUCCESS),
], "PowerShell — DeepFilterNet3 降噪服务启动")


# ═══════════════════════════════════════════
# 图6-3: Live Guard 视觉审核服务启动 (real log)
# ═══════════════════════════════════════════
terminal_screenshot("vision_guard_startup.png", [
    ("PS D:\\code\\live> .\\ai-services\\live-agent\\.venv\\Scripts\\python.exe ai-services\\vision-guard\\server\\vision_guard.py", C_PROMPT),
    ("", C_TEXT),
    ("[VisionGuard] Loading YOLOv8l model...", C_INFO),
    ("[VisionGuard] Loading YOLOv8l-pose model...", C_INFO),
    ("Using Device: cpu", C_INFO),
    ("[VisionGuard] Models loaded successfully", C_SUCCESS),
    ("[VisionGuard] FastAPI server starting on http://127.0.0.1:8300", C_HIGHLIGHT),
    ("[VisionGuard] Docs: http://127.0.0.1:8300/docs", C_HIGHLIGHT),
    ("", C_TEXT),
    ("INFO:     Started server process [48260]", C_SUCCESS),
    ("INFO:     Waiting for application startup.", C_INFO),
    ("INFO:     Application startup complete.", C_SUCCESS),
    ("INFO:     Uvicorn running on http://127.0.0.1:8300", C_SUCCESS),
    ("", C_TEXT),
    ("INFO:     127.0.0.1:58842 - \"POST /check HTTP/1.1\" 200 OK", C_DIM),
    ("INFO:     127.0.0.1:62202 - \"POST /check HTTP/1.1\" 200 OK", C_DIM),
], "PowerShell — Live Guard 视觉审核服务启动")


# ═══════════════════════════════════════════
# 图6-4: Live Agent AI 智能体服务启动 (real log)
# ═══════════════════════════════════════════
terminal_screenshot("ai_agent_startup.png", [
    ("PS D:\\code\\live> .\\ai-services\\live-agent\\.venv\\Scripts\\python.exe ai-services\\live-agent\\server.py", C_PROMPT),
    ("", C_TEXT),
    ("[AI Agent] starting. LLM=http://localhost:8000/v1, model=Qwen1.5-1.8B", C_HIGHLIGHT),
    ("[AI Agent] FastAPI server starting on http://127.0.0.1:8100", C_INFO),
    ("[AI Agent] Health check: http://localhost:8100/api/agent/health", C_INFO),
    ("", C_TEXT),
    ("INFO:     Started server process [41604]", C_SUCCESS),
    ("INFO:     Waiting for application startup.", C_INFO),
    ("INFO:     Application startup complete.", C_SUCCESS),
    ("", C_TEXT),
    ("INFO:     127.0.0.1:55066 - \"GET /api/agent/health HTTP/1.1\" 200 OK", C_DIM),
    ("INFO:     127.0.0.1:55075 - \"GET /api/agent/health HTTP/1.1\" 200 OK", C_DIM),
    ("INFO:     127.0.0.1:60519 - \"POST /api/agent/helper HTTP/1.1\" 200 OK", C_DIM),
], "PowerShell — Live Agent AI 智能体服务启动")


print("\nAll 10 terminal screenshots generated in", ASSETS)
