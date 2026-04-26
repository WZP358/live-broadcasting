package cn.imhtb.live.modules.live.guard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardCheckResult {

    private String status;

    private boolean safe;

    private boolean banned;

    private boolean skipped;

    private String reason;

    private String violationType;

    private String violationLabel;

    private Map<String, Object> evidence;
}
