package cn.imhtb.live.modules.live.agent;

import dev.langchain4j.service.UserMessage;

public interface LiveAgentAssistant {

    String chat(@UserMessage String question);
}
