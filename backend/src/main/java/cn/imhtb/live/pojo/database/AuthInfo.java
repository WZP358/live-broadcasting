package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author PinTeh
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@TableName("auth")
public class AuthInfo {

    @TableId
    private Integer id;

    private Integer userId;

    private String realName;

    private String positiveUrl;

    private String reverseUrl;

    private String handUrl;

    private String cardNo;

    private String rejectReason;

    private Integer status;

    private Integer operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
