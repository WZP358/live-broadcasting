package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_service_ticket")
public class CustomerServiceTicket {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String category;
    private String title;
    private String content;
    private Integer status;
    private Integer handlerId;
    private String reply;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime replyTime;
}
