package cn.imhtb.live.modules.live.vo;

import lombok.Data;

@Data
public class SearchResultVO {
    private Integer roomId;
    private String roomTitle;
    private String cover;
    private Integer status;
    private Boolean browserLive;
    private Integer popularity;
    private String anchorName;
    private String anchorAvatar;
    private String categoryName;
}
