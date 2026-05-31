package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_level")
public class UserLevel {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Long exp;              // 总经验值
    private Integer level;         // 当前等级 (1-100)
    private LocalDateTime updateTime;
}
