package cn.imhtb.live.modules.live.event;

public class LiveStartedEvent extends LiveEvent {

    public static final String TYPE = "live_started";

    public LiveStartedEvent(Integer roomId, Integer userId) {
        super(TYPE, roomId, userId);
    }
}
