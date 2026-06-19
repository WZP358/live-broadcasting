package cn.imhtb.live.modules.system.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SystemDemoStatus {

    private boolean enabled;

    private int roomCount;

    private int livingCount;

    private List<DemoRoomItem> rooms = new ArrayList<>();

    @Data
    public static class DemoRoomItem {
        private Integer id;
        private Integer userId;
        private String title;
        private String anchorName;
        private String categoryName;
        private String cover;
        private String playUrl;
        private Integer status;
    }
}
