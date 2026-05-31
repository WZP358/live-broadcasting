package cn.imhtb.live.modules.live.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class LiveEventBus {

    private final List<LiveEventObserver> observers = new CopyOnWriteArrayList<>();

    public void register(LiveEventObserver observer) {
        observers.add(observer);
        log.info("EventBus registered observer: {}", observer.getClass().getSimpleName());
    }

    public void unregister(LiveEventObserver observer) {
        observers.remove(observer);
        log.info("EventBus unregistered observer: {}", observer.getClass().getSimpleName());
    }

    public void publish(LiveEvent event) {
        log.info("EventBus publishing event: type={}, roomId={}", event.getEventType(), event.getRoomId());
        for (LiveEventObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                log.error("EventBus observer error: observer={}, event={}", observer.getClass().getSimpleName(), event.getEventType(), e);
            }
        }
    }
}
