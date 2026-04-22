package cn.imhtb.live.pojo.database;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author PinTeh
 * @date 2020/4/30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("role_menu")
public class RoleMenu implements Serializable {

    @TableId(value = "role_id", type = IdType.INPUT)
    private Integer roleId;

    private Integer menuId;

    @TableField(exist = false)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;

}
