package cn.imhtb.live.modules.live.event;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class LiveEvent {

    private final String eventType;
    private final Integer roomId;
    private final Integer userId;
    private final LocalDateTime timestamp;

    protected LiveEvent(String eventType, Integer roomId, Integer userId) {
        this.eventType = eventType;
        this.roomId = roomId;
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
    }
}
