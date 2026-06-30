package cn.imhtb.live.modules.live.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {

    private String answer;

    private String mode;

    private String model;

    private ToolInvocationTrace tool;
}
