package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.mappers.SettlementMapper;
import cn.imhtb.live.pojo.database.Settlement;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "收益结算接口")
@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SettlementController {

    private final SettlementMapper settlementMapper;

    @ApiOperation("我的结算列表")
    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "12") Integer limit) {
        Page<Settlement> pg = settlementMapper.selectPage(new Page<>(page, limit),
                new LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getUserId, UserHolder.getUserId())
                        .orderByDesc(Settlement::getPeriod));
        return ApiResponse.ofSuccess(pg);
    }

    @ApiOperation("结算汇总")
    @GetMapping("/summary")
    public ApiResponse<?> summary() {
        Integer userId = UserHolder.getUserId();
        List<Settlement> all = settlementMapper.selectList(new LambdaQueryWrapper<Settlement>()
                .eq(Settlement::getUserId, userId)
                .orderByDesc(Settlement::getPeriod));
        java.math.BigDecimal totalIncome = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalWithdrawable = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalWithdrawn = java.math.BigDecimal.ZERO;
        for (Settlement s : all) {
            if (s.getGiftIncome() != null) totalIncome = totalIncome.add(s.getGiftIncome());
            if (s.getWithdrawable() != null) totalWithdrawable = totalWithdrawable.add(s.getWithdrawable());
            if (s.getWithdrawn() != null) totalWithdrawn = totalWithdrawn.add(s.getWithdrawn());
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalIncome", totalIncome);
        result.put("totalWithdrawable", totalWithdrawable);
        result.put("totalWithdrawn", totalWithdrawn);
        result.put("totalSettlements", all.size());
        return ApiResponse.ofSuccess(result);
    }
}
