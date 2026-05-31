package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notification_pref")
public class NotificationPref {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Boolean liveStartEnabled;   // 开播提醒开关
    private Boolean followEnabled;      // 关注提醒开关
    private String dndStart;           // 免打扰开始 HH:mm
    private String dndEnd;             // 免打扰结束 HH:mm
}
