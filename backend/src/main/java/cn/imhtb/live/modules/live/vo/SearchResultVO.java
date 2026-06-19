package cn.imhtb.live.modules.live.vo;

import lombok.Data;

@Data
public class SearchResultVO {
    private Integer id;
    private String title;
    private Integer roomId;
    private String roomTitle;
    private String cover;
    private Integer status;
    private Boolean browserLive;
    private Integer popularity;
    private String anchorName;
    private String anchorAvatar;
    private Integer categoryId;
    private String categoryName;
    private UserInfoVO userInfo;
    private CategoryInfoVO categoryInfo;

    @Data
    public static class UserInfoVO {
        private Integer id;
        private String name;
        private String avatar;
    }

    @Data
    public static class CategoryInfoVO {
        private Integer id;
        private String name;
    }
}
