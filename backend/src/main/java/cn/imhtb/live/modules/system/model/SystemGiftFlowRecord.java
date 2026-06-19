package cn.imhtb.live.modules.system.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SystemGiftFlowRecord {

    private Long id;

    private Integer fromId;

    private String fromUserNickname;

    private String fromUserAvatar;

    private Integer toId;

    private String anchorNickname;

    private String anchorAvatar;

    private Integer roomId;

    private String roomTitle;

    private Integer presentId;

    private String presentName;

    private String presentIcon;

    private Integer number;

    private Integer type;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private LocalDateTime createTime;

}
