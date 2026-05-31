package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_moderator")
public class RoomModerator {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer roomId;
    private Integer userId;
    private Integer appointedBy;
    private LocalDateTime createTime;
}
