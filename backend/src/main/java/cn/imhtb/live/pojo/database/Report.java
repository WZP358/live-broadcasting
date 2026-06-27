package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer reporterId;      // 举报者
    private Integer targetUserId;    // 被举报用户
    private Integer roomId;          // 相关房间
    private String targetType;       // room / user / message
    private String targetId;         // 被举报对象ID
    private String reason;           // 举报原因
    private String description;      // 补充说明
    private Integer status;          // 0=待处理 1=已处理 2=已驳回
    private String handleResult;     // 处理结果
    private Integer handlerId;       // 处理人
    private LocalDateTime createTime;
    private LocalDateTime handleTime;

    @TableField(exist = false)
    private String roomTitle;

    @TableField(exist = false)
    private Integer roomDisabled;

    @TableField(exist = false)
    private Integer roomStatus;
}
