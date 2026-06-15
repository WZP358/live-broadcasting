"""PulseLive AI Agent service.

The service requires a configured OpenAI-compatible LLM. Moderation, summaries,
and helper answers must reflect the real AI integration status, so unavailable
or invalid model responses are reported as errors.
"""

import json
import os
from pathlib import Path

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field


BASE_DIR = Path(__file__).resolve().parent
ROOT_DIR = BASE_DIR.parents[1]
ENV_FILE = ROOT_DIR / ".env"

if ENV_FILE.exists():
    with open(ENV_FILE, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, _, value = line.partition("=")
                key = key.strip()
                value = value.strip().strip('"').strip("'")
                if key and value and key not in os.environ:
                    os.environ[key] = value


LLM_BASE_URL = os.environ.get("PULSELIVE_LLM_BASE_URL", "http://localhost:11434/v1")
LLM_MODEL = os.environ.get("PULSELIVE_LLM_MODEL", "qwen2.5:7b")
LLM_API_KEY = os.environ.get("PULSELIVE_LLM_API_KEY", "ollama")
LLM_TIMEOUT = float(os.environ.get("PULSELIVE_LLM_TIMEOUT", "30"))


app = FastAPI(
    title="PulseLive AI Agent",
    description="AI live assistant for sentiment analysis, live summaries, and platform help.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class LLMClient:
    """OpenAI-compatible chat client. The caller receives errors directly."""

    def __init__(self):
        self.base_url = LLM_BASE_URL.rstrip("/")
        self.model = LLM_MODEL
        self.api_key = LLM_API_KEY

    async def chat(
        self,
        system_prompt: str,
        user_message: str,
        temperature: float = 0.7,
        max_tokens: int = 512,
    ) -> str:
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
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()
                content = data["choices"][0]["message"]["content"].strip()
            except httpx.TimeoutException as exc:
                raise HTTPException(status_code=503, detail="AI model request timed out") from exc
            except httpx.HTTPStatusError as exc:
                raise HTTPException(
                    status_code=503,
                    detail=f"AI model service returned HTTP {exc.response.status_code}",
                ) from exc
            except Exception as exc:
                raise HTTPException(status_code=503, detail="AI model service unavailable") from exc

        if not content:
            raise HTTPException(status_code=503, detail="AI model returned an empty response")
        return content

    async def health_check(self) -> dict:
        try:
            headers = {}
            if self.api_key and self.api_key != "ollama":
                headers["Authorization"] = f"Bearer {self.api_key}"
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/models", headers=headers)
                if response.status_code != 200:
                    return {"status": "error", "message": f"HTTP {response.status_code}"}
                data = response.json()
                models = [item["id"] for item in data.get("data", []) if item.get("id")]
                return {
                    "status": "ok",
                    "endpoint": self.base_url,
                    "configured_model": self.model,
                    "available_models": models[:10],
                }
        except Exception as exc:
            return {"status": "unavailable", "message": str(exc)}


llm = LLMClient()


class SentimentRequest(BaseModel):
    messages: list = Field(default_factory=list)


class SentimentResult(BaseModel):
    overall: str
    score: float
    flags: list
    summary: str


class SummarizeRequest(BaseModel):
    title: str = ""
    category: str = ""
    anchor_name: str = ""
    highlights: list = Field(default_factory=list)


class SummarizeResult(BaseModel):
    summary: str
    tags: list
    welcome_msg: str


class HelperRequest(BaseModel):
    question: str


class HelperResult(BaseModel):
    answer: str


SENTIMENT_SYSTEM_PROMPT = """你是直播平台的内容安全 AI 助手。
请分析弹幕消息列表，返回严格 JSON：
{
  "overall": "positive|neutral|negative",
  "score": 0.5,
  "flags": [2, 5],
  "summary": "一句中文总结"
}
flags 是需要管理员关注的消息索引。只返回 JSON。"""


SUMMARIZE_SYSTEM_PROMPT = """你是直播平台的 AI 直播助手。
根据直播间信息生成严格 JSON：
{
  "summary": "2-3 句话直播摘要",
  "tags": ["标签1", "标签2"],
  "welcome_msg": "欢迎语"
}
只返回 JSON。"""


HELPER_SYSTEM_PROMPT = """你是 PulseLive 直播平台的 AI 客服助手。
用简洁中文回答平台使用问题，每次控制在 100 字以内。
不确定的问题要明确说明不知道，并建议联系人工客服。"""


def extract_json_object(response: str) -> dict:
    text = response.strip()
    if "```json" in text:
        text = text.split("```json", 1)[1].split("```", 1)[0]
    elif "```" in text:
        text = text.split("```", 1)[1].split("```", 1)[0]
    try:
        return json.loads(text.strip())
    except json.JSONDecodeError as exc:
        raise HTTPException(status_code=502, detail="AI model returned invalid JSON") from exc


@app.post("/api/agent/sentiment", response_model=SentimentResult)
async def analyze_sentiment(req: SentimentRequest):
    if not req.messages:
        raise HTTPException(status_code=400, detail="消息列表不能为空")

    lines = []
    for index, message in enumerate(req.messages):
        username = message.get("username", "匿名")
        content = message.get("content", "")
        lines.append(f"[{index}] {username}: {content}")

    response = await llm.chat(
        SENTIMENT_SYSTEM_PROMPT,
        "请分析以下弹幕消息：\n" + "\n".join(lines),
        temperature=0.3,
        max_tokens=300,
    )
    data = extract_json_object(response)
    try:
        return SentimentResult(
            overall=data["overall"],
            score=float(data["score"]),
            flags=data["flags"],
            summary=data["summary"],
        )
    except (KeyError, TypeError, ValueError) as exc:
        raise HTTPException(status_code=502, detail="AI sentiment result schema is invalid") from exc


@app.post("/api/agent/summarize", response_model=SummarizeResult)
async def generate_summary(req: SummarizeRequest):
    context = "\n".join(
        [
            "直播间信息：",
            f"- 标题：{req.title or '未设置'}",
            f"- 分类：{req.category or '未分类'}",
            f"- 主播：{req.anchor_name or '未知'}",
            f"- 关键事件：{'; '.join(req.highlights) if req.highlights else '暂无'}",
        ]
    )
    response = await llm.chat(SUMMARIZE_SYSTEM_PROMPT, context, temperature=0.7, max_tokens=300)
    data = extract_json_object(response)
    try:
        return SummarizeResult(
            summary=data["summary"],
            tags=data["tags"],
            welcome_msg=data["welcome_msg"],
        )
    except KeyError as exc:
        raise HTTPException(status_code=502, detail="AI summary result schema is invalid") from exc


@app.post("/api/agent/helper", response_model=HelperResult)
async def ask_helper(req: HelperRequest):
    if not req.question.strip():
        raise HTTPException(status_code=400, detail="问题不能为空")
    response = await llm.chat(HELPER_SYSTEM_PROMPT, req.question, temperature=0.5, max_tokens=200)
    return HelperResult(answer=response)


@app.get("/api/agent/health")
async def health():
    return {
        "service": "PulseLive AI Agent",
        "version": "1.0.0",
        "mode": "llm-required",
        "llm": await llm.health_check(),
    }


@app.get("/")
async def root():
    return {"message": "PulseLive AI Agent is running. Visit /docs for API docs."}


if __name__ == "__main__":
    import uvicorn

    port = int(os.environ.get("PULSELIVE_AGENT_PORT", "8100"))
    print(f"[AI Agent] starting. LLM={LLM_BASE_URL}, model={LLM_MODEL}", flush=True)
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
