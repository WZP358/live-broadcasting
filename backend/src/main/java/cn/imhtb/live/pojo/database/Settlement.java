package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement")
public class Settlement {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String period;           // 结算周期 如 2026-05
    private BigDecimal giftIncome;   // 礼物收入
    private BigDecimal platformFee;  // 平台抽成
    private BigDecimal netIncome;    // 主播实际收入
    private BigDecimal withdrawable; // 可提现余额
    private BigDecimal withdrawn;    // 已提现金额
    private Integer status;          // 0=待结算 1=已结算 2=已打款
    private LocalDateTime settleTime;
    private LocalDateTime createTime;
}
