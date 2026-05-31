package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
public class PrivateMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer fromUserId;
    private Integer toUserId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}
