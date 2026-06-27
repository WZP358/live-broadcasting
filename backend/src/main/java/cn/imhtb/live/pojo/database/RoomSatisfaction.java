package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_satisfaction")
public class RoomSatisfaction {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer roomId;
    private Integer userId;
    private Integer score;
    private LocalDateTime createTime;
}
