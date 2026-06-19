package cn.imhtb.live.pojo.vo.request;

import cn.imhtb.live.modules.live.vo.RewardReqVo;
import lombok.Data;

/**
 * @author PinTeh
 * @date 2020/3/25
 */
@Data
public class SendPresentRequest {

    private Integer pid;

    private Integer number;

    private Integer rid;

    private Integer vid;

    public RewardReqVo toRewardReqVo() {
        RewardReqVo req = new RewardReqVo();
        req.setPresentId(pid);
        req.setNumber(number);
        req.setRoomId(rid);
        req.setVid(vid);
        return req;
    }
}
