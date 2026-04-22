package cn.imhtb.live.modules.live.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.enums.PresentRewardTypeEnum;
import cn.imhtb.live.common.enums.StatusEnum;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.PresentMapper;
import cn.imhtb.live.mappers.PresentRewardMapper;
import cn.imhtb.live.mappers.RoomMapper;
import cn.imhtb.live.mappers.UserMapper;
import cn.imhtb.live.modules.live.service.ILiveGiftService;
import cn.imhtb.live.modules.live.service.IRoomIntimacyRankService;
import cn.imhtb.live.modules.live.vo.PresentRespVo;
import cn.imhtb.live.modules.live.vo.RewardReqVo;
import cn.imhtb.live.modules.live.vo.RewardRespVo;
import cn.imhtb.live.modules.server.netty.service.IRoomChatService;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import cn.imhtb.live.pojo.database.Present;
import cn.imhtb.live.pojo.database.PresentReward;
import cn.imhtb.live.pojo.database.Room;
import cn.imhtb.live.pojo.database.User;
import cn.imhtb.live.pojo.database.Wallet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LiveGiftServiceImpl implements ILiveGiftService {

    private final UserMapper userMapper;
    private final RoomMapper roomMapper;
    private final IWalletService walletService;
    private final PresentMapper presentMapper;
    private final IRoomChatService roomChatService;
    private final PresentRewardMapper presentRewardMapper;
    private final IRoomIntimacyRankService roomIntimacyRankService;

    @Override
    public List<PresentRespVo> list() {
        LambdaQueryWrapper<Present> wrapper = new LambdaQueryWrapper<Present>()
                .eq(Present::getDisabled, StatusEnum.YES.getCode())
                .orderByAsc(Present::getSort);
        return BeanUtil.copyToList(presentMapper.selectList(wrapper), PresentRespVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReward(RewardReqVo req) {
        Integer userId = UserHolder.getUserId();
        Present present = presentMapper.selectById(req.getPresentId());
        if (present == null) {
            throw new BusinessException("礼物不存在或已下架");
        }

        Wallet wallet = walletService.getWallet(userId);
        if (wallet == null || wallet.getId() == null || wallet.getId() <= 0) {
            throw new BusinessException("当前环境未初始化钱包表，暂不支持送礼功能");
        }

        BigDecimal totalPrice = present.getPrice().multiply(BigDecimal.valueOf(req.getNumber()));
        if (totalPrice.compareTo(wallet.getBalance()) > 0) {
            throw new BusinessException("余额不足，无法完成送礼");
        }

        User user = userMapper.selectById(userId);
        Room room = roomMapper.selectById(req.getRoomId());
        if (room == null) {
            throw new BusinessException("直播间不存在");
        }
        if (room.getUserId().equals(userId)) {
            throw new BusinessException("不能给自己的直播间送礼");
        }

        boolean decreased = walletService.decrease(userId, totalPrice);
        if (!decreased) {
            throw new BusinessException("余额发生变动，请重试");
        }
        walletService.increase(room.getUserId(), totalPrice);

        PresentReward presentReward = new PresentReward();
        presentReward.setFromId(userId);
        presentReward.setToId(room.getUserId());
        presentReward.setRoomId(req.getRoomId());
        presentReward.setType(PresentRewardTypeEnum.LIVE.getCode());
        presentReward.setUnitPrice(present.getPrice());
        presentReward.setPresentId(req.getPresentId());
        presentReward.setNumber(req.getNumber());
        presentReward.setTotalPrice(totalPrice);
        presentRewardMapper.insert(presentReward);

        roomIntimacyRankService.addGiftIntimacy(room.getId(), userId, totalPrice);
        String text = String.format("%s 送出了 %s x %d", user.getNickname(), present.getName(), req.getNumber());
        roomChatService.sendGiftMsg(text, room.getId(), userId, req.getPresentId());
    }

    @Override
    public PageData<RewardRespVo> rewardList(Integer page, Integer pageSize) {
        Page<RewardRespVo> pageParam = new Page<>(page, pageSize);
        QueryWrapper<RewardRespVo> wrapper = new QueryWrapper<>();
        wrapper.eq("pr.to_id", UserHolder.getUserId());
        wrapper.orderByDesc("pr.create_time", "pr.id");
        Page<RewardRespVo> result = presentRewardMapper.pageRewardRespVo(pageParam, wrapper);

        PageData<RewardRespVo> pageData = new PageData<>();
        pageData.setList(result.getRecords());
        pageData.setTotal(result.getTotal());
        return pageData;
    }
}
