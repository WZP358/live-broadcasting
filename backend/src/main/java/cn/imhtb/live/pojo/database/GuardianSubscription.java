package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("guardian_subscription")
public class GuardianSubscription {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer targetUserId;
    private Integer level;       // 1青铜 2白银 3黄金
    private BigDecimal amount;   // 当月支付金额
    private LocalDateTime expireTime;
    private Integer autoRenew;   // 0否 1是
    private Integer status;      // 0过期 1生效
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
