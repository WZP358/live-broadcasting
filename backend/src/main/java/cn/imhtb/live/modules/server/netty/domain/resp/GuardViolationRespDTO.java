package cn.imhtb.live.modules.server.netty.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 违规检测推送消息体
 *
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardViolationRespDTO {

    /**
     * 违规原因
     */
    private String reason;

    /**
     * 违规检测状态：REJECT / REVIEW
     */
    private String status;

    /**
     * 检测证据
     */
    private String violationType;

    private String violationLabel;

    private Map<String, Object> evidence;
}
