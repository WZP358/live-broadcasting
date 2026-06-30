package cn.imhtb.live.modules.live.agent;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AgentChatRequest {

    private String question;

    private String message;

    private Integer roomId;

    private Map<String, Object> context = new HashMap<>();

    public String getUserText() {
        if (question != null && !question.trim().isEmpty()) {
            return question.trim();
        }
        if (message != null && !message.trim().isEmpty()) {
            return message.trim();
        }
        return "";
    }
}
