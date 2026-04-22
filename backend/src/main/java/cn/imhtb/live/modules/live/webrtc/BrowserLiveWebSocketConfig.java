package cn.imhtb.live.modules.live.webrtc;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class BrowserLiveWebSocketConfig implements WebSocketConfigurer {

    private final BrowserLiveSignalHandler browserLiveSignalHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(browserLiveSignalHandler, "/ws/browser-live")
                .setAllowedOrigins("*");
    }
}
