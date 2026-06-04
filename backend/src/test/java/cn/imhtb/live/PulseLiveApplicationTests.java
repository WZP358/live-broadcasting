package cn.imhtb.live;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
@DisplayName("PulseLive 应用启动测试")
class PulseLiveApplicationTests {

    @Test
    @DisplayName("Spring 上下文应正常加载")
    void contextLoads() {
        // 验证 Spring 容器能正常启动
    }

    @Test
    @DisplayName("应用主类应可实例化")
    void applicationMainClassShouldBeInstantiable() {
        PulseLiveApplication app = new PulseLiveApplication();
        assertNotNull(app, "PulseLiveApplication 不应为 null");
    }
}
