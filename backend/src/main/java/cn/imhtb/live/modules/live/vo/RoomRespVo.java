package cn.imhtb.live.modules.live.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomRespVo {

    @ApiModelProperty("room id")
    private Integer id;

    @ApiModelProperty("room title")
    private String title;

    @ApiModelProperty("room cover")
    private String cover;

    @ApiModelProperty("play url")
    private String pullUrl;

    @ApiModelProperty("browser live status")
    private Boolean browserLive;

    @ApiModelProperty("room status")
    private Integer status;

    @ApiModelProperty("room intro")
    private String introduce;

    @ApiModelProperty("user info")
    private UserInfoVo userInfo;

    @ApiModelProperty("category info")
    private CategoryInfoVo categoryInfo;

    @Data
    @AllArgsConstructor
    public static class UserInfoVo {
        private Integer id;
        private String name;
        private String avatar;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryInfoVo {
        private Integer id;
        private String name;
    }
}
