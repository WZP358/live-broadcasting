package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("room_tag")
public class RoomTag {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer roomId;
    private String tagName;
}
