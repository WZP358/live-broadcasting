package cn.imhtb.live.modules.live.event;

import lombok.Getter;

@Getter
public class FollowedEvent extends LiveEvent {

    public static final String TYPE = "followed";

    private final Integer followerUserId;

    public FollowedEvent(Integer roomId, Integer followedUserId, Integer followerUserId) {
        super(TYPE, roomId, followedUserId);
        this.followerUserId = followerUserId;
    }
}
