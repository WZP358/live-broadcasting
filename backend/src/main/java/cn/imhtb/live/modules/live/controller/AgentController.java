package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.annotation.IgnoreToken;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 代理控制器 — 将前端请求转发给 Python AI Agent 服务。
 *
 * <p>三个子 Agent：
 * <ul>
 *   <li>POST /api/v1/agent/sentiment  — 弹幕情感哨兵</li>
 *   <li>POST /api/v1/agent/summarize  — 直播智囊</li>
 *   <li>POST /api/v1/agent/helper     — 平台小助手</li>
 * </ul>
 * </p>
 *
 * @author PulseLive AI Team
 */
@Slf4j
@Api(tags = "AI 智能助手")
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final RestTemplate restTemplate;
    private final String agentBaseUrl;

    public AgentController(
            @Value("${pulselive.agent.url:http://localhost:8100}") String agentBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.agentBaseUrl = agentBaseUrl;
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
    public ApiResponse<?> askHelper(@RequestBody Map<String, Object> body) {
        return forward("/api/agent/helper", body);
    }

    @IgnoreToken
    @ApiOperation("AI Agent 健康检查")
    @GetMapping("/health")
    public ApiResponse<?> health() {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    agentBaseUrl + "/api/agent/health", Map.class);
            return ApiResponse.ofSuccess(resp.getBody());
        } catch (Exception e) {
            log.warn("[AI Agent] 健康检查失败: {}", e.getMessage());
            return ApiResponse.ofSuccess(Map.of(
                    "service", "PulseLive AI Agent",
                    "status", "unavailable",
                    "message", "AI Agent 服务未启动，请先运行 ai-services/live-agent/server.py"
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
}
