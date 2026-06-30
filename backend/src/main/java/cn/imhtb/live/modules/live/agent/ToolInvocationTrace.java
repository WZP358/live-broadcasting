package cn.imhtb.live.modules.live.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInvocationTrace {

    private boolean called;

    private String toolName;

    private Map<String, Object> arguments;

    private Object result;

    private String source;
}
