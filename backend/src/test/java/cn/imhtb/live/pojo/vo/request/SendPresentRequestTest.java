package cn.imhtb.live.pojo.vo.request;

import cn.imhtb.live.modules.live.vo.RewardReqVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SendPresentRequest 旧送礼接口兼容")
class SendPresentRequestTest {

    @Test
    @DisplayName("When: 旧接口请求转换, Then: 映射为统一送礼请求")
    void shouldConvertToRewardReqVo() {
        SendPresentRequest request = new SendPresentRequest();
        request.setPid(10);
        request.setRid(20);
        request.setVid(30);
        request.setNumber(2);

        RewardReqVo result = request.toRewardReqVo();

        assertEquals(10, result.getPresentId());
        assertEquals(20, result.getRoomId());
        assertEquals(30, result.getVid());
        assertEquals(2, result.getNumber());
    }
}
