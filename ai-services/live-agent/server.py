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


LLM_BASE_URL = os.environ.get("PULSELIVE_LLM_BASE_URL", "http://localhost:8000/v1")
LLM_MODEL = os.environ.get("PULSELIVE_LLM_MODEL", "Qwen1.5-1.8B")
LLM_API_KEY = os.environ.get("PULSELIVE_LLM_API_KEY", "")
LLM_TIMEOUT = float(os.environ.get("PULSELIVE_LLM_TIMEOUT", "30"))
LLM_STOP_SEQUENCES = [
    "\nsystem\n",
    "\nuser\n",
    "\nassistant\n",
    "\nSystem:",
    "\nUser:",
    "\nAssistant:",
    "\nHuman:",
    "\nAI:",
    "\nAI：",
    "\n用户回答",
    "<|im_end|>",
    "<|im_start|>",
]


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
        response_format: dict = None,
    ) -> str:
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message},
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
            "stop": LLM_STOP_SEQUENCES,
        }
        if response_format:
            payload["response_format"] = response_format

        async with httpx.AsyncClient(timeout=LLM_TIMEOUT, trust_env=False) as client:
            try:
                response = await client.post(
                    f"{self.base_url}/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()
                content = clean_model_output(data["choices"][0]["message"]["content"])
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
            if self.api_key:
                headers["Authorization"] = f"Bearer {self.api_key}"
            async with httpx.AsyncClient(timeout=5.0, trust_env=False) as client:
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


def clean_model_output(text: str) -> str:
    if not text:
        return ""
    content = text.strip()
    for marker in LLM_STOP_SEQUENCES:
        pos = content.find(marker)
        if pos >= 0:
            content = content[:pos].strip()
    return content


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
    context: dict = Field(default_factory=dict)


class HelperResult(BaseModel):
    answer: str


SENTIMENT_SYSTEM_PROMPT = """你是直播平台的内容安全 AI 助手。
请分析弹幕消息列表，返回严格 JSON 对象：
{
  "overall": "positive|neutral|negative",
  "score": 0.5,
  "flags": [2, 5],
  "summary": "一句中文总结"
}
score 范围是 -1 到 1，越积极越接近 1，越消极越接近 -1。
flags 是需要管理员关注的消息索引。只返回 JSON。"""


SENTIMENT_RESPONSE_FORMAT = {
    "type": "json_schema",
    "json_schema": {
        "name": "sentiment_result",
        "schema": {
            "type": "object",
            "properties": {
                "overall": {"type": "string", "enum": ["positive", "neutral", "negative"]},
                "score": {"type": "number", "minimum": -1, "maximum": 1},
                "flags": {"type": "array", "items": {"type": "integer"}},
                "summary": {"type": "string"},
            },
            "required": ["overall", "score", "flags", "summary"],
        },
    },
}


SUMMARY_RESPONSE_FORMAT = {
    "type": "json_schema",
    "json_schema": {
        "name": "live_summary_result",
        "schema": {
            "type": "object",
            "properties": {
                "summary": {"type": "string"},
                "tags": {"type": "array", "items": {"type": "string"}},
                "welcome_msg": {"type": "string"},
            },
            "required": ["summary", "tags", "welcome_msg"],
        },
    },
}


SUMMARIZE_SYSTEM_PROMPT = """你是直播平台的 AI 直播助手。
根据直播间信息生成严格 JSON：
{
  "summary": "2-3 句话直播摘要",
  "tags": ["标签1", "标签2"],
  "welcome_msg": "欢迎语"
}
只返回 JSON。"""


HELPER_SYSTEM_PROMPT = """你是 PulseLive 直播平台的 AI 客服助手。
只输出给用户看的最终答案，不要复述上下文，不要输出 system/user/assistant/Human/AI 等角色标记。
用简洁中文回答平台使用问题，每次控制在 100 字以内。
你可以根据用户当前页面上下文给出具体操作建议。
如果上下文里有直播间标题、主播、分类、开播状态，必须优先基于这些事实回答。
不要假装看到了实时视频画面；无法从资料判断的画面动作要明确说明。
不确定的问题要明确说明不知道，并建议联系人工客服。"""


JSON_REPAIR_SYSTEM_PROMPT = """你是 JSON 结构修复器。
把输入内容转换为符合目标结构的 JSON 对象。
不要解释，不要输出 Markdown，只返回 JSON。"""


def extract_json_object(response: str) -> dict:
    text = response.strip()
    candidates = []
    if "```json" in text:
        candidates.append(text.split("```json", 1)[1].split("```", 1)[0])
    elif "```" in text:
        candidates.append(text.split("```", 1)[1].split("```", 1)[0])

    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        candidates.append(text[start : end + 1])
    candidates.append(text)

    for candidate in candidates:
        try:
            data = json.loads(candidate.strip())
            if isinstance(data, dict):
                return data
        except json.JSONDecodeError:
            continue
    raise HTTPException(status_code=502, detail="AI model returned invalid JSON")


async def extract_or_repair_json(response: str, response_format: dict) -> dict:
    try:
        return extract_json_object(response)
    except HTTPException as first_error:
        schema = response_format.get("json_schema", {}).get("schema", {})
        repair_message = "\n".join(
            [
                "目标 JSON Schema：",
                json.dumps(schema, ensure_ascii=False),
                "原始模型输出：",
                response[:2000],
            ]
        )
        repaired = await llm.chat(
            JSON_REPAIR_SYSTEM_PROMPT,
            repair_message,
            temperature=0,
            max_tokens=260,
            response_format=response_format,
        )
        try:
            return extract_json_object(repaired)
        except HTTPException:
            raise first_error


def normalize_sentiment_overall(value, score: float = None) -> str:
    if isinstance(value, dict):
        value = value.get("overall") or value.get("sentiment") or value.get("label")
    if isinstance(value, (int, float)):
        if value > 0.2:
            return "positive"
        if value < -0.2:
            return "negative"
        return "neutral"
    text = str(value or "").strip().lower()
    if text in {"positive", "积极", "正向", "活跃", "好", "pos"}:
        return "positive"
    if text in {"negative", "消极", "负向", "风险", "违规", "差", "neg"}:
        return "negative"
    if score is not None:
        if score > 0.25:
            return "positive"
        if score < -0.25:
            return "negative"
    return "neutral"


def normalize_sentiment_score(value) -> float:
    if isinstance(value, dict):
        value = value.get("score")
    try:
        score = float(value)
    except (TypeError, ValueError):
        return 0.0
    return max(-1.0, min(1.0, score))


def normalize_sentiment_flags(value, message_count: int) -> list:
    if isinstance(value, (int, float)):
        value = [] if int(value) == 0 else [int(value)]
    if isinstance(value, str):
        value = [] if value.strip().lower() in {"", "none", "null", "no", "无"} else [value]
    if not isinstance(value, list):
        return []
    flags = []
    for item in value:
        try:
            index = int(item)
        except (TypeError, ValueError):
            continue
        if 0 <= index < message_count and index not in flags:
            flags.append(index)
    return flags[:8]


def normalize_short_text(value, default: str, limit: int = 120) -> str:
    if value is None:
        return default
    if isinstance(value, (dict, list)):
        text = json.dumps(value, ensure_ascii=False)
    else:
        text = str(value)
    text = text.strip()
    return (text or default)[:limit]


def build_sentiment_result(data: dict, message_count: int) -> SentimentResult:
    nested_overall = data.get("overall") if isinstance(data.get("overall"), dict) else {}
    score = normalize_sentiment_score(data.get("score", nested_overall.get("score")))
    overall = normalize_sentiment_overall(data.get("overall", nested_overall.get("overall")), score)
    flags = normalize_sentiment_flags(data.get("flags", nested_overall.get("flags")), message_count)
    summary = normalize_short_text(
        data.get("summary", nested_overall.get("summary")),
        "弹幕整体氛围正常。",
    )
    return SentimentResult(overall=overall, score=score, flags=flags, summary=summary)


def normalize_tags(value) -> list:
    if isinstance(value, str):
        raw_tags = value.replace("，", ",").replace("、", ",").split(",")
    elif isinstance(value, list):
        raw_tags = value
    else:
        raw_tags = []
    tags = []
    for item in raw_tags:
        text = str(item).strip()
        if text and text not in tags:
            tags.append(text[:12])
    return tags[:5]


def build_helper_context(context: dict) -> str:
    if not context:
        return "当前页面上下文：无。"
    page = context.get("page") or {}
    actions = page.get("actions") or []
    room = page.get("room") or {}
    studio_room = page.get("studioRoom") or {}
    conversation = context.get("conversation") or []
    lines = [
        "当前页面上下文：",
        f"- 页面：{page.get('title') or '未知页面'}",
        f"- 路径：{page.get('path') or '未知路径'}",
        f"- 登录状态：{'已登录' if page.get('loggedIn') else '未登录'}",
        f"- 管理员：{'是' if page.get('isAdmin') else '否'}",
    ]
    if actions:
        lines.append("- 当前页可执行操作：" + "、".join(str(item) for item in actions))
    if room:
        lines.extend(
            [
                "当前直播间资料：",
                f"- 房间ID：{room.get('id') or page.get('roomId') or '未知'}",
                f"- 标题：{room.get('title') or '未设置'}",
                f"- 主播：{room.get('anchorName') or '未知'}",
                f"- 分类：{room.get('categoryName') or '未分类'}",
                f"- 状态：{room.get('statusText') or '未知'}",
                f"- 公告：{room.get('notice') or '无'}",
                f"- 简介：{room.get('introduce') or '无'}",
            ]
        )
    if studio_room:
        lines.extend(
            [
                "主播工作台房间资料：",
                f"- 房间ID：{studio_room.get('id') or '未知'}",
                f"- 标题：{studio_room.get('title') or '未设置'}",
                f"- 主播：{studio_room.get('anchorName') or '未知'}",
                f"- 分类：{studio_room.get('categoryName') or '未分类'}",
                f"- 状态：{studio_room.get('statusText') or '未知'}",
            ]
        )
    if conversation:
        recent = []
        for item in conversation[-6:]:
            role = item.get("role", "user")
            content = str(item.get("content", "")).strip()
            if content:
                recent.append(f"{role}: {content[:160]}")
        if recent:
            lines.append("最近对话：")
            lines.extend(f"- {item}" for item in recent)
    return "\n".join(lines)


@app.post("/api/agent/sentiment", response_model=SentimentResult)
async def analyze_sentiment(req: SentimentRequest):
    valid_messages = []
    for message in req.messages:
        content = str(message.get("content") or message.get("text") or "").strip()
        if not content:
            continue
        valid_messages.append(
            {
                "username": str(message.get("username") or message.get("nickname") or "匿名").strip() or "匿名",
                "content": content[:200],
            }
        )

    if not valid_messages:
        raise HTTPException(status_code=400, detail="消息列表不能为空")

    message_payload = []
    for index, message in enumerate(valid_messages):
        message_payload.append(
            {
                "index": index,
                "username": message.get("username", "匿名"),
                "content": message.get("content", ""),
            }
        )

    response = await llm.chat(
        SENTIMENT_SYSTEM_PROMPT,
        "请分析以下弹幕 JSON 数组：\n" + json.dumps(message_payload, ensure_ascii=False),
        temperature=0.1,
        max_tokens=220,
        response_format=SENTIMENT_RESPONSE_FORMAT,
    )
    data = await extract_or_repair_json(response, SENTIMENT_RESPONSE_FORMAT)
    return build_sentiment_result(data, len(valid_messages))


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
    response = await llm.chat(
        SUMMARIZE_SYSTEM_PROMPT,
        context,
        temperature=0.5,
        max_tokens=260,
        response_format=SUMMARY_RESPONSE_FORMAT,
    )
    data = await extract_or_repair_json(response, SUMMARY_RESPONSE_FORMAT)
    try:
        return SummarizeResult(
            summary=normalize_short_text(data.get("summary"), "直播摘要生成完成。", 240),
            tags=normalize_tags(data.get("tags")),
            welcome_msg=normalize_short_text(data.get("welcome_msg"), "欢迎来到直播间。", 120),
        )
    except KeyError as exc:
        raise HTTPException(status_code=502, detail="AI summary result schema is invalid") from exc


@app.post("/api/agent/helper", response_model=HelperResult)
async def ask_helper(req: HelperRequest):
    if not req.question.strip():
        raise HTTPException(status_code=400, detail="问题不能为空")
    user_message = (
        "请只回答最后一行的问题，不要复述页面上下文。\n"
        f"{build_helper_context(req.context)}\n\n"
        f"问题：{req.question}"
    )
    response = await llm.chat(HELPER_SYSTEM_PROMPT, user_message, temperature=0.3, max_tokens=120)
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
