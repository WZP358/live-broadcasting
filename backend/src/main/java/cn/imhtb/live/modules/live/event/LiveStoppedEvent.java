package cn.imhtb.live.modules.live.event;

public class LiveStoppedEvent extends LiveEvent {

    public static final String TYPE = "live_stopped";

    public LiveStoppedEvent(Integer roomId) {
        super(TYPE, roomId, null);
    }
}
