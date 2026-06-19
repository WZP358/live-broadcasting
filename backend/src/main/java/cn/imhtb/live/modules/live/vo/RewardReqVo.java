package cn.imhtb.live.modules.live.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author pinteh
 * @date 2024/9/20
 */
@Data
public class RewardReqVo {

    @NotNull
    @ApiModelProperty("礼物编号")
    private Integer presentId;

    @NotNull
    @Min(value = 1, message = "礼物数量必须大于 0")
    @Max(value = 999, message = "单次礼物数量不能超过 999")
    @ApiModelProperty("礼物数量")
    private Integer number;

    @NotNull
    @ApiModelProperty("房间号")
    private Integer roomId;

    private Integer vid;

}
