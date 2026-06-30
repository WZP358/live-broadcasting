package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.modules.live.agent.AgentChatRequest;
import cn.imhtb.live.modules.live.agent.AgentChatResponse;
import cn.imhtb.live.modules.live.agent.LangChainLiveAgentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Api(tags = "AI 智能助手")
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final RestTemplate restTemplate;
    private final String agentBaseUrl;
    private final LangChainLiveAgentService langChainLiveAgentService;

    public AgentController(
            @Value("${pulselive.agent.url:http://localhost:8100}") String agentBaseUrl,
            LangChainLiveAgentService langChainLiveAgentService) {
        this.restTemplate = new RestTemplate();
        this.agentBaseUrl = agentBaseUrl;
        this.langChainLiveAgentService = langChainLiveAgentService;
    }

    @IgnoreToken
    @ApiOperation("弹幕情感分析")
    @PostMapping("/sentiment")
    public ApiResponse<?> analyzeSentiment(@RequestBody Map<String, Object> body) {
        return forward("/api/agent/sentiment", body);
    }

    @IgnoreToken
    @ApiOperation("生成直播摘要")
    @PostMapping("/summarize")
    public ApiResponse<?> generateSummary(@RequestBody Map<String, Object> body) {
        return forward("/api/agent/summarize", body);
    }

    @IgnoreToken
    @ApiOperation("平台小助手问答")
    @PostMapping("/helper")
    public ApiResponse<AgentChatResponse> askHelper(@RequestBody Map<String, Object> body) {
        return ApiResponse.ofSuccess(langChainLiveAgentService.chat(toAgentChatRequest(body)));
    }

    @IgnoreToken
    @ApiOperation("LangChain4j 工具调用智能体问答")
    @PostMapping("/tool-chat")
    public ApiResponse<AgentChatResponse> toolChat(@RequestBody AgentChatRequest request) {
        return ApiResponse.ofSuccess(langChainLiveAgentService.chat(request));
    }

    @IgnoreToken
    @ApiOperation("AI Agent 健康检查")
    @GetMapping("/health")
    public ApiResponse<?> health() {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    agentBaseUrl + "/api/agent/health", Map.class);
            Map<String, Object> body = resp.getBody();
            if (body != null) {
                body.put("javaAgent", Map.of(
                        "status", "ok",
                        "mode", "langchain4j-tool-agent",
                        "tools", new String[]{
                                "get_live_room_audience_stats",
                                "get_live_room_detail",
                                "list_living_rooms"
                        }
                ));
            }
            return ApiResponse.ofSuccess(body);
        } catch (Exception e) {
            log.warn("[AI Agent] 健康检查失败: {}", e.getMessage());
            return ApiResponse.ofSuccess(Map.of(
                    "service", "PulseLive AI Agent",
                    "status", "unavailable",
                    "message", "AI Agent 服务未启动，请先运行 ai-services/live-agent/server.py",
                    "javaAgent", Map.of(
                            "status", "ok",
                            "mode", "langchain4j-tool-agent",
                            "tools", new String[]{
                                    "get_live_room_audience_stats",
                                    "get_live_room_detail",
                                    "list_living_rooms"
                            }
                    )
            ));
        }
    }

    private ApiResponse<?> forward(String path, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    agentBaseUrl + path, entity, Map.class);
            return ApiResponse.ofSuccess(resp.getBody());
        } catch (Exception e) {
            log.error("[AI Agent] 请求失败: path={}, error={}", path, e.getMessage());
            return ApiResponse.ofError("AI Agent 服务暂不可用: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private AgentChatRequest toAgentChatRequest(Map<String, Object> body) {
        AgentChatRequest request = new AgentChatRequest();
        if (body == null) {
            return request;
        }
        Object question = body.get("question");
        Object message = body.get("message");
        if (question != null) {
            request.setQuestion(String.valueOf(question));
        }
        if (message != null) {
            request.setMessage(String.valueOf(message));
        }
        request.setRoomId(toInteger(body.get("roomId")));
        Object context = body.get("context");
        if (context instanceof Map) {
            request.setContext((Map<String, Object>) context);
        }
        return request;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && ((String) value).matches("\\d+")) {
            return Integer.parseInt((String) value);
        }
        return null;
    }
}
