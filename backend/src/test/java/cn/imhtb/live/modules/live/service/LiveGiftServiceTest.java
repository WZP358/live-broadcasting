package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.mappers.PresentMapper;
import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.impl.LiveGiftServiceImpl;
import cn.imhtb.live.modules.live.vo.RewardReqVo;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiveGiftService 礼物业务规则")
class LiveGiftServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private IWalletService walletService;
    @Mock
    private PresentMapper presentMapper;
    @Mock
    private IRoomChatService roomChatService;
    @Mock
    private PresentRewardMapper presentRewardMapper;
    @Mock
    private IRoomIntimacyRankService roomIntimacyRankService;

    @InjectMocks
    private LiveGiftServiceImpl liveGiftService;

    @Test
    @DisplayName("Given: 送礼请求为空, When: 创建礼物流水, Then: 返回明确业务错误")
    void shouldRejectNullRewardRequest() {
        BusinessException exception = assertThrows(BusinessException.class, () -> liveGiftService.createReward(null));
        assertEquals("送礼参数不能为空", exception.getMsg());
    }

    @Test
    @DisplayName("Given: 未选择礼物, When: 创建礼物流水, Then: 返回明确业务错误")
    void shouldRejectMissingGiftId() {
        RewardReqVo req = new RewardReqVo();
        req.setRoomId(1);
        req.setNumber(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> liveGiftService.createReward(req));
        assertEquals("请选择礼物", exception.getMsg());
    }

    @Test
    @DisplayName("Given: 未关联直播间, When: 创建礼物流水, Then: 返回明确业务错误")
    void shouldRejectMissingRoomId() {
        RewardReqVo req = new RewardReqVo();
        req.setPresentId(1);
        req.setNumber(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> liveGiftService.createReward(req));
        assertEquals("直播间不存在", exception.getMsg());
    }
}
