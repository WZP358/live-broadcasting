package cn.imhtb.live.modules.live.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
public class LangChainAgentConfig {

    @Bean
    public ChatModel pulseLiveChatModel(
            @Value("${pulselive.llm.base-url:${PULSELIVE_LLM_BASE_URL:http://localhost:8000/v1}}") String baseUrl,
            @Value("${pulselive.llm.model:${PULSELIVE_LLM_MODEL:Qwen1.5-1.8B}}") String modelName,
            @Value("${pulselive.llm.api-key:${PULSELIVE_LLM_API_KEY:}}") String apiKey,
            @Value("${pulselive.llm.timeout-seconds:${PULSELIVE_LLM_TIMEOUT:60}}") Integer timeoutSeconds) {
        return OpenAiChatModel.builder()
                .httpClientBuilder(new OkHttpLangChainHttpClientBuilder())
                .baseUrl(baseUrl)
                .apiKey(StringUtils.hasText(apiKey) ? apiKey : "not-needed")
                .modelName(modelName)
                .temperature(0.1)
                .maxTokens(512)
                .timeout(Duration.ofSeconds(timeoutSeconds == null ? 60 : timeoutSeconds))
                .maxRetries(0)
                .build();
    }

    @Bean
    public LiveAgentAssistant nativeToolCallingAssistant(ChatModel pulseLiveChatModel, LiveRoomAgentTools tools) {
        return AiServices.builder(LiveAgentAssistant.class)
                .chatModel(pulseLiveChatModel)
                .tools(tools)
                .systemMessage("你是 PulseLive 直播平台智能体。涉及业务数据时必须调用工具，不能编造直播间人数、状态、关注数或房间列表。")
                .maxToolCallingRoundTrips(2)
                .build();
    }
}
