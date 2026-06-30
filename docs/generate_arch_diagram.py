import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

plt.rcParams["font.family"] = "Microsoft YaHei"
plt.rcParams["font.size"] = 11

fig, ax = plt.subplots(1, 1, figsize=(20, 14))
ax.set_xlim(0, 20)
ax.set_ylim(0, 14)
ax.axis("off")
ax.set_facecolor("#f5f7fa")

# ── Color palette ──
C_FRONT = "#4A90D9"       # blue - frontend
C_ADMIN = "#7B68EE"       # purple - admin
C_BACKEND = "#E67E22"     # orange - backend
C_WS = "#F39C12"          # yellow - websocket
C_LIVE = "#27AE60"        # green - media server
C_AI = "#E74C3C"          # red - AI services
C_DATA = "#2C3E50"        # dark - data layer
C_THIRD = "#95A5A6"       # grey - 3rd party
C_BOX_BG = "#FFFFFF"
C_ARROW = "#555555"
C_BORDER = "#CCCCCC"

ARROW_PROPS = dict(arrowstyle="->", color=C_ARROW, lw=1.8, connectionstyle="arc3,rad=0.0")

def draw_box(ax, x, y, w, h, color, title, subtitle="", fontsize_title=13, fontsize_sub=9):
    """Draw a rounded box with title and optional subtitle."""
    box = FancyBboxPatch((x - w/2, y - h/2), w, h,
                         boxstyle="round,pad=0.08", facecolor=C_BOX_BG,
                         edgecolor=color, linewidth=2.5, zorder=2)
    ax.add_patch(box)
    # header bar
    bar = FancyBboxPatch((x - w/2 + 0.05, y + h/2 - 0.52), w - 0.1, 0.5,
                         boxstyle="round,pad=0.04", facecolor=color,
                         edgecolor="none", zorder=3)
    ax.add_patch(bar)
    ax.text(x, y + h/2 - 0.27, title, ha="center", va="center",
            fontsize=fontsize_title, fontweight="bold", color="white", zorder=4)
    if subtitle:
        ax.text(x, y - h/2 + 0.2, subtitle, ha="center", va="center",
                fontsize=fontsize_sub, color="#666666", zorder=4)

def draw_arrow(ax, x1, y1, x2, y2, label="", color=C_ARROW, lw=1.5, style="arc3,rad=0.0"):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="->", color=color, lw=lw, connectionstyle=style))
    if label:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        ax.text(mx + 0.15, my + 0.1, label, fontsize=8, color=color, style="italic")

def draw_layer_label(ax, x, y, text):
    ax.text(x, y, text, fontsize=10, fontweight="bold", color="#888888",
            ha="left", va="center", rotation=90)

# ═══════════════════════════════════════════
# LAYOUT COORDINATES
# ═══════════════════════════════════════════

# Row 1 (top): Frontend layer  y=11.5
# Row 2: Backend + Services    y=7.5
# Row 3: Data layer            y=3.5
# Row 4: 3rd party             y=0.8

# --- LAYER LABELS ---
draw_layer_label(ax, 0.4, 11.5, "展示层")
draw_layer_label(ax, 0.4, 7.8, "服务层")
draw_layer_label(ax, 0.4, 3.5, "数据层")
draw_layer_label(ax, 0.4, 1.0, "第三方服务")

# --- FRONTEND (row 1) ---
draw_box(ax, 5.5, 11.5, 3.8, 2.2, C_FRONT, "用户端前端", "Vue 3 + Vite\nAnt Design Vue\nflv.js / hls.js\nWebRTC / WebSocket", 13, 8)
draw_box(ax, 11.5, 11.5, 3.8, 2.2, C_ADMIN, "管理后台", "Vue 2\n用户管理 / 审核\n数据统计 / 日志", 13, 8)

# --- BACKEND (row 2, center) ---
draw_box(ax, 5.5, 7.5, 4.5, 2.8, C_BACKEND, "Spring Boot 后端", "REST API (8088)\nNetty WebSocket (10022)\n直播管理 / 聊天 / 钱包\n礼物打赏 / 权限管理", 12, 8)

# --- MEDIA SERVER ---
draw_box(ax, 12.5, 9.3, 3.8, 1.6, C_LIVE, "直播媒体服务器", "Node.js\nRTMP 推流 (1935)\nHTTP-FLV 拉流 (8080)", 11, 8)

# --- AI SERVICES ---
draw_box(ax, 15.8, 6.2, 3.5, 2.8, C_AI, "AI 服务", "降噪: DeepFilterNet3\n审核: YOLOv8 Live Guard\n助手: LLM Live Agent", 11, 8)

# Nested AI boxes
for i, (name, port, yo) in enumerate([
    ("降噪 DeepFilterNet3", "ws:18765", 7.4),
    ("审核 Live Guard", ":8300", 6.3),
    ("助手 Live Agent", ":8100", 5.2),
]):
    bx = FancyBboxPatch((14.2, yo - 0.3), 2.6, 0.5, boxstyle="round,pad=0.03",
                         facecolor="#fdecea", edgecolor=C_AI, linewidth=1, zorder=5)
    ax.add_patch(bx)
    ax.text(15.5, yo - 0.05, name, ha="center", va="center", fontsize=7, color=C_AI, zorder=6)
    ax.text(15.5, yo - 0.32, port, ha="center", va="center", fontsize=6, color="#999", zorder=6)

# --- DATA LAYER (row 3) ---
data_items = [
    (3.5, 3.5, "MySQL", "8.x\n业务数据\nant-live"),
    (7.5, 3.5, "Redis", "缓存 / 会话\n在线状态\n限流"),
    (11.5, 3.5, "MinIO", "对象存储\n文件/封面\nlive.file.bucket"),
]
for x, y, title, sub in data_items:
    draw_box(ax, x, y, 3.0, 2.2, C_DATA, title, sub, 13, 8)

# --- 3RD PARTY (row 4) ---
tp_items = [
    (4.0, 1.0, "阿里云短信", "验证码发送"),
    (9.0, 1.0, "支付宝沙箱", "钱包充值 / 回调"),
    (14.0, 1.0, "QQ 邮箱 SMTP", "邮件验证码"),
]
for x, y, title, sub in tp_items:
    draw_box(ax, x, y, 3.4, 1.2, C_THIRD, title, sub, 11, 7)

# ═══════════════════════════════════════════
# ARROWS / CONNECTIONS
# ═══════════════════════════════════════════

# Frontend → Backend
draw_arrow(ax, 4.5, 10.4, 4.5, 8.9, "HTTP/REST\n/api 代理", "#4A90D9")
# Admin → Backend
draw_arrow(ax, 10.5, 10.4, 6.5, 8.9, "HTTP/REST\n/admin", "#7B68EE")

# Frontend → Media (FLV pull)
draw_arrow(ax, 7.0, 11.0, 11.5, 10.3, "HTTP-FLV\n/live-stream", "#27AE60")

# Frontend → Backend WS
draw_arrow(ax, 6.8, 11.0, 7.0, 8.7, "WebSocket\n/ws-netty", "#F39C12")

# Backend → Data
draw_arrow(ax, 4.3, 6.0, 3.5, 4.7, "JDBC", "#2C3E50")
draw_arrow(ax, 5.5, 6.0, 7.5, 4.7, "Jedis", "#2C3E50")
draw_arrow(ax, 6.5, 6.0, 11.5, 4.7, "S3 API", "#2C3E50")

# Backend → AI (guard check)
draw_arrow(ax, 7.5, 7.2, 14.0, 7.2, "HTTP /check", "#E74C3C")

# Backend → Media (RTMP callback)
draw_arrow(ax, 7.5, 8.3, 11.0, 9.6, "RTMP Callback", "#27AE60")

# Backend → Third party
draw_arrow(ax, 4.2, 5.4, 4.0, 1.7, "HTTPS", "#95A5A6")
draw_arrow(ax, 5.5, 5.6, 9.0, 1.7, "HTTPS", "#95A5A6")
draw_arrow(ax, 6.0, 5.5, 14.0, 1.7, "SMTPS", "#95A5A6")

# Media → Backend (stream events)
draw_arrow(ax, 12.5, 8.5, 7.5, 6.5, "on_pub_start\n/stop/update", "#27AE60")

# ── LEGEND ──
legend_y = 13.6
legend_items = [
    (C_FRONT, "前端展示层"), (C_BACKEND, "业务服务"), (C_LIVE, "媒体服务"),
    (C_AI, "AI 模型服务"), (C_DATA, "数据存储"), (C_THIRD, "第三方服务"),
]
for i, (color, label) in enumerate(legend_items):
    lx = 10.0 + i * 1.7
    ax.add_patch(FancyBboxPatch((lx, legend_y), 1.3, 0.25,
                                boxstyle="round,pad=0.02", facecolor=color, edgecolor="none", zorder=5))
    ax.text(lx + 0.65, legend_y + 0.12, label, ha="center", va="center", fontsize=7, color="#555")

ax.text(19.5, 13.6, "图1-1", ha="right", va="center", fontsize=14, fontweight="bold", color="#333")

plt.tight_layout(pad=0.5)
plt.savefig(r"D:\code\live\docs\assets\arch_diagram.png", dpi=180, bbox_inches="tight",
            facecolor=fig.get_facecolor(), edgecolor="none")
plt.close()
print("Done: docs/assets/arch_diagram.png")
