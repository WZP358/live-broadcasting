package cn.imhtb.live.common.utils;

import io.jsonwebtoken.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil JWT 令牌工具")
class JwtUtilTest {

    @Test
    @DisplayName("生成的 token 可以解析出正确的 account")
    void shouldCreateAndParseToken() {
        String token = JwtUtil.createTokenByParams(1, "测试用户", "test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = JwtUtil.getUsernameByToken(token);
        assertEquals("test@example.com", username);
    }

    @Test
    @DisplayName("带有 Bearer 前缀的 token 可以正确解析")
    void shouldParseTokenWithBearerPrefix() {
        String rawToken = JwtUtil.createTokenByParams(2, "用户2", "user2@test.com");
        assertTrue(rawToken.startsWith("Bearer "), "token 应以 Bearer 开头");

        String username = JwtUtil.getUsernameByToken(rawToken);
        assertEquals("user2@test.com", username);
    }

    @Test
    @DisplayName("解析 null token 应抛出 NullPointerException")
    void shouldNotNullForNullToken() {
        assertThrows(NullPointerException.class, () -> {
            // token.replace(...) on null will NPE
            JwtUtil.getUsernameByToken(null);
        });
    }

    @Test
    @DisplayName("解析篡改过的 token 应抛出 SignatureException")
    void shouldThrowForTamperedToken() {
        String validToken = JwtUtil.createTokenByParams(3, "张三", "zhangsan@test.com");
        String tamperedToken = validToken.substring(0, validToken.length() - 4) + "xxxx";
        assertThrows(SignatureException.class, () -> {
            JwtUtil.getUsernameByToken(tamperedToken);
        });
    }

    @Test
    @DisplayName("相同参数生成的 token 可以解析出相同 account")
    void shouldParseSameAccountFromMultipleTokens() {
        String token1 = JwtUtil.createTokenByParams(1, "用户A", "a@test.com");
        String token2 = JwtUtil.createTokenByParams(1, "用户A", "a@test.com");
        assertEquals("a@test.com", JwtUtil.getUsernameByToken(token1));
        assertEquals("a@test.com", JwtUtil.getUsernameByToken(token2));
    }

    @Test
    @DisplayName("用户名参数不影响解析出的 account")
    void shouldUseAccountAsSubject() {
        String token = JwtUtil.createTokenByParams(10, "显示名", "real_account@test.com");
        assertEquals("real_account@test.com", JwtUtil.getUsernameByToken(token));
    }
}
