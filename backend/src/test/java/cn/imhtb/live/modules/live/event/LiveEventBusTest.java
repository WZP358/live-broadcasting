package cn.imhtb.live.modules.live.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LiveEventBus 观察者模式事件总线")
class LiveEventBusTest {

    private LiveEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new LiveEventBus();
    }

    @Test
    @DisplayName("注册的观察者应收到发布的事件")
    void shouldNotifyRegisteredObserver() {
        List<LiveEvent> received = new ArrayList<>();
        LiveEventObserver observer = event -> received.add(event);
        eventBus.register(observer);

        LiveStartedEvent event = new LiveStartedEvent(1, 100);
        eventBus.publish(event);

        assertEquals(1, received.size());
        assertEquals("live_started", received.get(0).getEventType());
        assertEquals(1, received.get(0).getRoomId());
    }

    @Test
    @DisplayName("取消注册后观察者不再收到事件")
    void shouldNotNotifyUnregisteredObserver() {
        AtomicInteger callCount = new AtomicInteger(0);
        LiveEventObserver observer = event -> callCount.incrementAndGet();

        eventBus.register(observer);
        eventBus.publish(new LiveStartedEvent(1, 100));
        assertEquals(1, callCount.get());

        eventBus.unregister(observer);
        eventBus.publish(new LiveStartedEvent(2, 200));
        assertEquals(1, callCount.get()); // still 1, not called again
    }

    @Test
    @DisplayName("多个观察者应全部收到事件")
    void shouldNotifyAllObservers() {
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);

        eventBus.register(event -> count1.incrementAndGet());
        eventBus.register(event -> count2.incrementAndGet());

        eventBus.publish(new LiveStoppedEvent(5));

        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
    }

    @Test
    @DisplayName("一个观察者抛异常不应影响其他观察者")
    void shouldIsolateObserverErrors() {
        AtomicInteger goodCount = new AtomicInteger(0);
        LiveEventObserver badObserver = event -> {
            throw new RuntimeException("observer internal error");
        };
        LiveEventObserver goodObserver = event -> goodCount.incrementAndGet();

        eventBus.register(badObserver);
        eventBus.register(goodObserver);

        // should not throw
        assertDoesNotThrow(() -> eventBus.publish(new LiveStartedEvent(3, 300)));
        assertEquals(1, goodCount.get(), "正常观察者仍应收到事件");
    }

    @Test
    @DisplayName("没有注册观察者时 publish 不应抛异常")
    void shouldNotThrowWhenNoObservers() {
        assertDoesNotThrow(() -> eventBus.publish(new LiveStoppedEvent(9)));
    }

    @Test
    @DisplayName("LiveStartedEvent 应包含正确的 userId 和 roomId")
    void shouldCreateStartedEventWithCorrectFields() {
        LiveStartedEvent event = new LiveStartedEvent(42, 999);
        assertEquals("live_started", event.getEventType());
        assertEquals(42, event.getRoomId());
        assertEquals(999, event.getUserId());
    }

    @Test
    @DisplayName("LiveStoppedEvent 应包含 roomId")
    void shouldCreateStoppedEventWithRoomId() {
        LiveStoppedEvent event = new LiveStoppedEvent(77);
        assertEquals("live_stopped", event.getEventType());
        assertEquals(77, event.getRoomId());
    }

    @Test
    @DisplayName("FollowedEvent 应包含关注双方 userId")
    void shouldCreateFollowedEventWithBothUserIds() {
        FollowedEvent event = new FollowedEvent(10, 100, 200);
        assertEquals("followed", event.getEventType());
        assertEquals(10, event.getRoomId());
        assertEquals(100, event.getUserId());
        assertEquals(200, event.getFollowerUserId());
    }
}
