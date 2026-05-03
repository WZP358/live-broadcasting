package cn.imhtb.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.modules.live.service.LiveLifecycleService;
import cn.imhtb.live.pojo.vo.lal.PubVO;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * lal live callbacks.
 *
 * @author pinteh
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LalController {

    private final LiveLifecycleService liveLifecycleService;

    @PostMapping("/lolo")
    public ApiResponse<?> lolo(@RequestBody Map<String, String> map) {
        return ApiResponse.ofSuccess(map);
    }

    @PostMapping("/on_pub_start")
    public void pubStart(@RequestBody PubVO pubVo) {
        log.info("on_pub_start callback: {}", JSON.toJSONString(pubVo));
        Integer roomId = parseRoomId(pubVo);
        if (roomId != null) {
            liveLifecycleService.markLiveStarted(roomId, null);
        }
    }

    @PostMapping("/on_pub_stop")
    public void pubStop(@RequestBody PubVO pubVo) {
        log.info("on_pub_stop callback: {}", JSON.toJSONString(pubVo));
        Integer roomId = parseRoomId(pubVo);
        if (roomId != null) {
            liveLifecycleService.markLiveStopped(roomId);
        }
    }

    @PostMapping("/on_update")
    public void pubStart(@RequestBody String pubVo) {
        log.info("on_update callback: {}", JSON.toJSONString(pubVo));
    }

    private Integer parseRoomId(PubVO pubVo) {
        if (pubVo == null || !StringUtils.hasText(pubVo.getStreamName())) {
            return null;
        }
        try {
            return Integer.valueOf(pubVo.getStreamName());
        } catch (NumberFormatException e) {
            log.warn("ignore lal callback with invalid streamName={}", pubVo.getStreamName());
            return null;
        }
    }
}
