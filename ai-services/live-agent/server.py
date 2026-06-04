"""
PulseLive AI Agent — 智能直播助手服务

三个子 Agent：
  1. 弹幕情感哨兵 (Barrage Sentiment Guard)
  2. 直播智囊 (Live Brain)
  3. 平台小助手 (Platform Helper)

共用 OpenAI-compatible API 后端（支持 DeepSeek / OpenAI / Ollama 等）。
配置优先从项目根目录 .env 读取。
"""
import os
import json
import time
import asyncio
from typing import Optional
from pathlib import Path

# ── 自动加载项目根目录 .env ──
BASE_DIR = Path(__file__).resolve().parent
ROOT_DIR = BASE_DIR.parents[1]  # 项目根目录 d:\code\live
ENV_FILE = ROOT_DIR / ".env"
if ENV_FILE.exists():
    with open(ENV_FILE, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, _, val = line.partition("=")
                key = key.strip()
                val = val.strip().strip('"').strip("'")
                if key and val and key not in os.environ:
                    os.environ[key] = val

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# ─── 配置 ─────────────────────────────────────────────────
LLM_BASE_URL = os.environ.get("PULSELIVE_LLM_BASE_URL", "http://localhost:11434/v1")
LLM_MODEL = os.environ.get("PULSELIVE_LLM_MODEL", "qwen2.5:7b")
LLM_API_KEY = os.environ.get("PULSELIVE_LLM_API_KEY", "ollama")  # Ollama 不需要真实 key
LLM_TIMEOUT = float(os.environ.get("PULSELIVE_LLM_TIMEOUT", "30"))

app = FastAPI(
    title="PulseLive AI Agent",
    description="智能直播助手 — 弹幕情感分析 · 直播摘要 · 平台问答",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── LLM 客户端 ────────────────────────────────────────────

class LLMClient:
    """OpenAI-compatible API 客户端，支持 Ollama / OpenAI / DeepSeek 等。"""

    def __init__(self):
        self.base_url = LLM_BASE_URL.rstrip("/")
        self.model = LLM_MODEL
        self.api_key = LLM_API_KEY

    async def chat(self, system_prompt: str, user_message: str,
                   temperature: float = 0.7, max_tokens: int = 512) -> str:
        """发送对话请求，返回模型回复文本。"""
        headers = {"Content-Type": "application/json"}
        if self.api_key and self.api_key != "ollama":
            headers["Authorization"] = f"Bearer {self.api_key}"

        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message},
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        async with httpx.AsyncClient(timeout=LLM_TIMEOUT) as client:
            try:
                resp = await client.post(
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=payload,
                )
                resp.raise_for_status()
                data = resp.json()
                return data["choices"][0]["message"]["content"].strip()
            except httpx.TimeoutException:
                return "[AI Agent 超时] 模型响应过慢，请稍后重试。"
            except Exception as e:
                print(f"[LLM] 调用失败: {e}", flush=True)
                return ""

    async def health_check(self) -> dict:
        """检查 LLM 服务是否可用。"""
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                resp = await client.get(f"{self.base_url}/models")
                if resp.status_code == 200:
                    data = resp.json()
                    models = [m["id"] for m in data.get("data", [])]
                    return {
                        "status": "ok",
                        "endpoint": self.base_url,
                        "configured_model": self.model,
                        "available_models": models[:10],
                    }
                return {"status": "error", "message": f"HTTP {resp.status_code}"}
        except Exception as e:
            return {"status": "unavailable", "message": str(e)}


llm = LLMClient()

# ─── 请求/响应模型 ─────────────────────────────────────────

class SentimentRequest(BaseModel):
    messages: list  # [{"username": "小明", "content": "太棒了！"}]

class SentimentResult(BaseModel):
    overall: str       # positive / neutral / negative
    score: float       # -1.0 ~ 1.0
    flags: list        # 需要标记的消息索引列表
    summary: str       # 一句话总结

class SummarizeRequest(BaseModel):
    title: str = ""
    category: str = ""
    anchor_name: str = ""
    highlights: list = []  # 关键事件列表

class SummarizeResult(BaseModel):
    summary: str       # 直播摘要
    tags: list         # 推荐标签
    welcome_msg: str   # 欢迎语

class HelperRequest(BaseModel):
    question: str

class HelperResult(BaseModel):
    answer: str

# ─── Agent 1: 弹幕情感哨兵 ─────────────────────────────────

SENTIMENT_SYSTEM_PROMPT = """你是一个直播平台的内容安全 AI 助手（弹幕情感哨兵）。
你的任务是分析弹幕消息列表，完成以下工作：

1. 判断整体情感倾向：positive（正面）、neutral（中性）、negative（负面）
2. 给出情感分数（-1.0 到 1.0，负数表示负面）
3. 标记有问题的消息（广告、辱骂、引战、色情暗示等），返回它们的索引
4. 用一句话中文总结当前弹幕氛围

返回格式必须是严格的 JSON：
{
  "overall": "positive|neutral|negative",
  "score": 0.5,
  "flags": [2, 5],
  "summary": "观众对主播的精彩操作反响热烈，整体氛围积极。"
}

只返回 JSON，不要多余的文字。"""


@app.post("/api/agent/sentiment", response_model=SentimentResult)
async def analyze_sentiment(req: SentimentRequest):
    """分析弹幕情感倾向。"""
    if not req.messages:
        raise HTTPException(400, "消息列表不能为空")

    # 构建消息文本
    lines = []
    for i, msg in enumerate(req.messages):
        username = msg.get("username", "匿名")
        content = msg.get("content", "")
        lines.append(f"[{i}] {username}: {content}")

    user_msg = "请分析以下弹幕消息：\n" + "\n".join(lines)

    response = await llm.chat(SENTIMENT_SYSTEM_PROMPT, user_msg, temperature=0.3, max_tokens=300)

    try:
        # 尝试提取 JSON（模型可能包裹在 ```json ... ``` 中）
        json_str = response
        if "```json" in response:
            json_str = response.split("```json")[1].split("```")[0]
        elif "```" in response:
            json_str = response.split("```")[1].split("```")[0]
        data = json.loads(json_str.strip())
        return SentimentResult(
            overall=data.get("overall", "neutral"),
            score=float(data.get("score", 0)),
            flags=data.get("flags", []),
            summary=data.get("summary", "无法分析"),
        )
    except (json.JSONDecodeError, KeyError, ValueError):
        # 降级：返回原始响应作为 summary
        return SentimentResult(
            overall="neutral",
            score=0.0,
            flags=[],
            summary=response[:200] if response else "AI 服务暂时不可用",
        )


# ─── Agent 2: 直播智囊 ─────────────────────────────────────

SUMMARIZE_SYSTEM_PROMPT = """你是一个直播平台的 AI 直播智囊。
根据提供的直播间信息和关键事件，生成以下内容：

1. summary: 一段 2-3 句话的直播精彩摘要
2. tags: 3-5 个推荐标签（如：技术干货、前端进阶、实战教学）
3. welcome_msg: 一句热情的新观众欢迎语（20字以内）

返回格式必须是严格的 JSON：
{
  "summary": "...",
  "tags": ["标签1", "标签2"],
  "welcome_msg": "..."
}

只返回 JSON，不要多余的文字。"""


@app.post("/api/agent/summarize", response_model=SummarizeResult)
async def generate_summary(req: SummarizeRequest):
    """生成直播摘要和欢迎语。"""
    context = f"""直播间信息：
- 标题：{req.title or '未设置'}
- 分类：{req.category or '未分类'}
- 主播：{req.anchor_name or '未知'}
- 关键事件：{'; '.join(req.highlights) if req.highlights else '暂无'}"""

    response = await llm.chat(SUMMARIZE_SYSTEM_PROMPT, context, temperature=0.7, max_tokens=300)

    try:
        json_str = response
        if "```json" in response:
            json_str = response.split("```json")[1].split("```")[0]
        elif "```" in response:
            json_str = response.split("```")[1].split("```")[0]
        data = json.loads(json_str.strip())
        return SummarizeResult(
            summary=data.get("summary", response[:300]),
            tags=data.get("tags", []),
            welcome_msg=data.get("welcome_msg", "欢迎来到直播间！"),
        )
    except (json.JSONDecodeError, KeyError):
        return SummarizeResult(
            summary=response[:300] if response else "AI 服务暂时不可用",
            tags=[],
            welcome_msg="欢迎来到直播间！",
        )


# ─── Agent 3: 平台小助手 ───────────────────────────────────

HELPER_SYSTEM_PROMPT = """你是 PulseLive 直播平台的 AI 助手「小脉」。
用友好、简洁的中文回答用户关于平台使用的问题。

平台功能简介：
- 可以观看直播、发送弹幕、赠送礼物
- 支持关注主播、查看历史记录
- 个人中心可以充值、查看账单、管理关注
- 主播可以开通直播间、设置分类和公告
- 支持浏览器直接开播（WebRTC）
- 有 AI 实时降噪功能提升音质
- 有人工智能内容审核保障安全

回答要求：
- 每次回答控制在 100 字以内
- 语气亲切友好
- 不确定的问题诚实说不知道，建议联系客服
- 如果是问候，热情回应"""


@app.post("/api/agent/helper", response_model=HelperResult)
async def ask_helper(req: HelperRequest):
    """平台小助手问答。"""
    if not req.question.strip():
        raise HTTPException(400, "问题不能为空")

    response = await llm.chat(HELPER_SYSTEM_PROMPT, req.question, temperature=0.5, max_tokens=200)
    return HelperResult(answer=response or "小脉正在思考，请稍后再问~")


# ─── 系统端点 ──────────────────────────────────────────────

@app.get("/api/agent/health")
async def health():
    """健康检查 + LLM 状态。"""
    llm_status = await llm.health_check()
    return {
        "service": "PulseLive AI Agent",
        "version": "1.0.0",
        "llm": llm_status,
    }


@app.get("/")
async def root():
    return {"message": "PulseLive AI Agent 运行中。访问 /docs 查看 API 文档。"}


# ─── 启动 ──────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PULSELIVE_AGENT_PORT", "8100"))
    print(f"[AI Agent] 启动中... LLM: {LLM_BASE_URL} 模型: {LLM_MODEL}", flush=True)
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
