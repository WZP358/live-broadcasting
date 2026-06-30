package cn.imhtb.live.modules.live.agent;

import cn.imhtb.live.common.exception.BusinessException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LangChainLiveAgentService {

    private static final String TOOL_AUDIENCE_STATS = "get_live_room_audience_stats";
    private static final String TOOL_ROOM_DETAIL = "get_live_room_detail";
    private static final String TOOL_LIVING_ROOMS = "list_living_rooms";

    private final ChatModel pulseLiveChatModel;
    private final LiveRoomAgentTools tools;

    @Value("${pulselive.llm.model:${PULSELIVE_LLM_MODEL:Qwen1.5-1.8B}}")
    private String modelName;

    public AgentChatResponse chat(AgentChatRequest request) {
        String text = request == null ? "" : request.getUserText();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("请输入要咨询的问题。");
        }

        ToolCall toolCall = chooseTool(request, text);
        if (toolCall != null) {
            String toolResult = executeTool(toolCall);
            Object parsedResult = parseResult(toolResult);
            String answer = buildToolAnswer(toolCall.toolName, parsedResult);
            return AgentChatResponse.builder()
                    .answer(answer)
                    .mode("langchain4j-tool-agent")
                    .model(modelName)
                    .tool(ToolInvocationTrace.builder()
                            .called(true)
                            .toolName(toolCall.toolName)
                            .arguments(toolCall.arguments)
                            .result(parsedResult)
                            .source(toolCall.source)
                            .build())
                    .build();
        }

        String answer = askGeneralQuestion(request, text);
        return AgentChatResponse.builder()
                .answer(answer)
                .mode("langchain4j-chat")
                .model(modelName)
                .tool(ToolInvocationTrace.builder()
                        .called(false)
                        .source("no_tool_needed")
                        .build())
                .build();
    }

    private ToolCall chooseTool(AgentChatRequest request, String text) {
        ToolCall plannedByModel = planWithModel(request, text);
        if (plannedByModel != null && isSupportedTool(plannedByModel.toolName)) {
            return plannedByModel;
        }
        return planWithBusinessGuardrail(request, text);
    }

    private ToolCall planWithModel(AgentChatRequest request, String text) {
        try {
            Integer roomId = resolveRoomId(request, text);
            String prompt = "You are a tool router for a live streaming platform. "
                    + "Never invent business data. If a question needs platform data, choose a tool. "
                    + "Return only one JSON object like {\"action\":\"tool\",\"tool\":\"get_live_room_audience_stats\",\"args\":{\"roomId\":23}} "
                    + "or {\"action\":\"answer\",\"answer\":\"short answer\"}.\n"
                    + "Context roomId: " + (roomId == null ? "null" : roomId) + "\n"
                    + "Tools:\n"
                    + "- get_live_room_audience_stats(roomId): online users/viewers/audience/popularity/count of one room.\n"
                    + "- get_live_room_detail(roomId): room title, anchor, category, status, intro and follow count.\n"
                    + "- list_living_rooms(limit): current live room list.\n"
                    + "Question: " + text + "\n"
                    + "JSON:";
            String raw = cleanModelText(pulseLiveChatModel.chat(prompt));
            JSONObject json = extractFirstJsonObject(raw);
            if (json == null || !"tool".equalsIgnoreCase(json.getString("action"))) {
                return null;
            }
            String toolName = json.getString("tool");
            JSONObject args = json.getJSONObject("args");
            Map<String, Object> arguments = new LinkedHashMap<>();
            if (args != null) {
                arguments.putAll(args);
            }

            if (TOOL_AUDIENCE_STATS.equals(toolName) || TOOL_ROOM_DETAIL.equals(toolName)) {
                if (roomId == null) {
                    return null;
                }
                arguments.put("roomId", roomId);
            }
            if (TOOL_LIVING_ROOMS.equals(toolName) && !arguments.containsKey("limit")) {
                arguments.put("limit", resolveLimit(text));
            }
            return new ToolCall(toolName, arguments, "llm_plan");
        } catch (Exception e) {
            log.debug("LangChain4j tool planning skipped: {}", e.getMessage());
            return null;
        }
    }

    private ToolCall planWithBusinessGuardrail(AgentChatRequest request, String text) {
        Integer roomId = resolveRoomId(request, text);
        String normalized = text.toLowerCase();
        if (containsAny(normalized, "多少人", "几个人", "在线人数", "在线", "观众", "人数", "人气", "热度", "viewer", "audience", "online", "popularity")) {
            if (roomId != null) {
                return toolCall(TOOL_AUDIENCE_STATS, "business_guardrail", "roomId", roomId);
            }
            return toolCall(TOOL_LIVING_ROOMS, "business_guardrail", "limit", resolveLimit(text));
        }
        if (containsAny(normalized, "房间信息", "直播间信息", "房间详情", "直播间详情", "主播是谁", "什么分类", "状态", "介绍", "标题")) {
            if (roomId != null) {
                return toolCall(TOOL_ROOM_DETAIL, "business_guardrail", "roomId", roomId);
            }
        }
        if (containsAny(normalized, "正在直播", "直播列表", "直播间列表", "哪些直播", "有哪些直播", "开播列表", "living rooms")) {
            return toolCall(TOOL_LIVING_ROOMS, "business_guardrail", "limit", resolveLimit(text));
        }
        return null;
    }

    private String executeTool(ToolCall toolCall) {
        Integer roomId = intArg(toolCall.arguments, "roomId");
        if (TOOL_AUDIENCE_STATS.equals(toolCall.toolName)) {
            return tools.getLiveRoomAudienceStats(roomId);
        }
        if (TOOL_ROOM_DETAIL.equals(toolCall.toolName)) {
            return tools.getLiveRoomDetail(roomId);
        }
        if (TOOL_LIVING_ROOMS.equals(toolCall.toolName)) {
            return tools.listLivingRooms(intArg(toolCall.arguments, "limit"));
        }
        throw new BusinessException("模型请求了暂不支持的工具：" + toolCall.toolName);
    }

    private String askGeneralQuestion(AgentChatRequest request, String text) {
        try {
            String prompt = "你是 PulseLive 直播平台的小脉 AI 助手。回答要简洁、自然、不要编造业务数据。"
                    + "如果用户问直播间人数、房间状态、关注数、直播列表，应提醒用户提供具体直播间或进入对应页面后再问。\n"
                    + "页面上下文：" + JSON.toJSONString(request == null ? null : request.getContext()) + "\n"
                    + "用户问题：" + text;
            String answer = cleanModelText(pulseLiveChatModel.chat(prompt));
            if (StringUtils.hasText(answer)) {
                return answer;
            }
        } catch (Exception e) {
            log.warn("LangChain4j general answer failed: {}", e.getMessage());
        }
        return "我现在可以帮你查询直播间在线人数、房间详情和正在直播列表。你可以问：这个直播间现在有多少人？";
    }

    private String buildToolAnswer(String toolName, Object parsedResult) {
        JSONObject result = parsedResult instanceof JSONObject ? (JSONObject) parsedResult : null;
        if (result == null) {
            return "工具已经执行，但返回结果暂时无法解析。";
        }
        if (!Boolean.TRUE.equals(result.getBoolean("success"))) {
            return result.getString("message") == null ? "没有查询到对应业务数据。" : result.getString("message");
        }

        if (TOOL_AUDIENCE_STATS.equals(toolName)) {
            JSONObject room = result.getJSONObject("room");
            String title = room == null ? "该直播间" : defaultText(room.getString("title"), "该直播间");
            long onlineCount = result.getLongValue("onlineCount");
            long chatOnlineCount = result.getLongValue("chatOnlineCount");
            int signalViewerCount = result.getIntValue("signalViewerCount");
            boolean anchorOnline = Boolean.TRUE.equals(result.getBoolean("anchorOnline"));
            long followCount = result.getLongValue("followCount");
            return "直播间「" + title + "」当前在线约 " + onlineCount + " 人。"
                    + "其中聊天室在线 " + chatOnlineCount + " 人，音视频观众连接 " + signalViewerCount + " 个，"
                    + "主播" + (anchorOnline ? "在线" : "暂未检测到音视频连接") + "，关注数 " + followCount + "。";
        }

        if (TOOL_ROOM_DETAIL.equals(toolName)) {
            JSONObject room = result.getJSONObject("room");
            if (room == null) {
                return "没有查询到这个直播间的详情。";
            }
            return "直播间「" + defaultText(room.getString("title"), "未命名") + "」由 "
                    + defaultText(room.getString("anchorName"), "未知主播") + " 创建，分类是「"
                    + defaultText(room.getString("categoryName"), "未分类") + "」，当前状态为"
                    + defaultText(room.getString("statusText"), "未知") + "，关注数 "
                    + result.getLongValue("followCount") + "。";
        }

        if (TOOL_LIVING_ROOMS.equals(toolName)) {
            int total = result.getIntValue("total");
            if (total <= 0) {
                return "当前没有检测到正在直播的直播间。";
            }
            StringBuilder builder = new StringBuilder("当前共有 ").append(total).append(" 个直播间正在开播。");
            for (Object item : result.getJSONArray("rooms")) {
                if (item instanceof JSONObject) {
                    JSONObject room = (JSONObject) item;
                    builder.append("「")
                            .append(defaultText(room.getString("title"), "未命名直播间"))
                            .append("」");
                    if (room.getString("anchorName") != null) {
                        builder.append("主播：").append(room.getString("anchorName"));
                    }
                    builder.append("，在线 ").append(room.getLongValue("onlineCount")).append(" 人；");
                }
            }
            return builder.toString();
        }

        return "工具已经执行完成：" + JSON.toJSONString(parsedResult);
    }

    private Integer resolveRoomId(AgentChatRequest request, String text) {
        if (request != null && request.getRoomId() != null && request.getRoomId() > 0) {
            return request.getRoomId();
        }
        Integer fromContext = roomIdFromContext(request == null ? null : request.getContext());
        if (fromContext != null) {
            return fromContext;
        }

        Integer beforeName = firstInt(text, "(?:房间|直播间|room|#)\\s*(\\d{1,9})");
        if (beforeName != null) {
            return beforeName;
        }
        return firstInt(text, "(\\d{1,9})\\s*(?:号)?\\s*(?:房间|直播间)");
    }

    @SuppressWarnings("unchecked")
    private Integer roomIdFromContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        Integer direct = objectToInt(context.get("roomId"));
        if (direct != null) {
            return direct;
        }
        Object page = context.get("page");
        if (page instanceof Map) {
            Integer pageRoomId = objectToInt(((Map<String, Object>) page).get("roomId"));
            if (pageRoomId != null) {
                return pageRoomId;
            }
            Object room = ((Map<String, Object>) page).get("room");
            if (room instanceof Map) {
                Integer roomId = objectToInt(((Map<String, Object>) room).get("id"));
                if (roomId != null) {
                    return roomId;
                }
            }
        }
        Object room = context.get("room");
        if (room instanceof Map) {
            return objectToInt(((Map<String, Object>) room).get("id"));
        }
        return null;
    }

    private Integer resolveLimit(String text) {
        Integer limit = firstInt(text, "(\\d{1,2})\\s*(?:个|条|间)");
        if (limit == null) {
            return 5;
        }
        return Math.max(1, Math.min(limit, 10));
    }

    private ToolCall toolCall(String toolName, String source, String key, Object value) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put(key, value);
        return new ToolCall(toolName, arguments, source);
    }

    private boolean isSupportedTool(String toolName) {
        return TOOL_AUDIENCE_STATS.equals(toolName) || TOOL_ROOM_DETAIL.equals(toolName) || TOOL_LIVING_ROOMS.equals(toolName);
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Integer intArg(Map<String, Object> arguments, String key) {
        return arguments == null ? null : objectToInt(arguments.get(key));
    }

    private Integer objectToInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && ((String) value).matches("\\d+")) {
            return Integer.parseInt((String) value);
        }
        return null;
    }

    private Integer firstInt(String text, String regex) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private Object parseResult(String toolResult) {
        try {
            return JSON.parseObject(toolResult);
        } catch (Exception e) {
            return toolResult;
        }
    }

    private JSONObject extractFirstJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return JSON.parseObject(text.substring(start, i + 1));
                }
            }
        }
        return null;
    }

    private String cleanModelText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        String[] stops = {"\nsystem\n", "\nuser\n", "\nassistant\n", "\nSystem:", "\nUser:", "\nAssistant:", "\nHuman:"};
        for (String stop : stops) {
            int index = cleaned.indexOf(stop);
            if (index >= 0) {
                cleaned = cleaned.substring(0, index).trim();
            }
        }
        return cleaned.replace("```json", "").replace("```", "").trim();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static class ToolCall {
        private final String toolName;
        private final Map<String, Object> arguments;
        private final String source;

        private ToolCall(String toolName, Map<String, Object> arguments, String source) {
            this.toolName = toolName;
            this.arguments = arguments;
            this.source = source;
        }
    }
}
