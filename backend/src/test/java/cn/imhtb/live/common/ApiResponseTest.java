package cn.imhtb.live.common;

import cn.imhtb.live.common.exception.base.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponse 统一响应结构")
class ApiResponseTest {

    @Test
    @DisplayName("ofSuccess() 返回 code=0, msg=Success")
    void shouldReturnSuccessCodeAndMessage() {
        ApiResponse<?> res = ApiResponse.ofSuccess();
        assertEquals(0, res.getCode());
        assertEquals("Success", res.getMsg());
    }

    @Test
    @DisplayName("isSuccess() 当 code=0 时返回 true")
    void shouldReturnTrueWhenCodeIsZero() {
        assertTrue(ApiResponse.ofSuccess().isSuccess());
        assertFalse(ApiResponse.ofError().isSuccess());
    }

    @Test
    @DisplayName("ofError() 默认使用 SERVICE_ERROR 的 code 和中文消息")
    void shouldUseServiceErrorCodeAndChineseMessage() {
        ApiResponse<?> res = ApiResponse.ofError();
        assertEquals(CommonErrorCode.SERVICE_ERROR.getCode(), res.getCode());
        assertEquals("系统服务异常，请稍后重试", res.getMsg());
    }

    @Test
    @DisplayName("ofError(msg) 保持 code=1 并使用自定义消息")
    void shouldKeepServiceErrorCodeWithCustomMessage() {
        ApiResponse<?> res = ApiResponse.ofError("参数错误");
        assertEquals(1, res.getCode());
        assertEquals("参数错误", res.getMsg());
    }

    @Test
    @DisplayName("ofError(code, msg) 使用自定义 code 和 msg")
    void shouldUseCustomCodeAndMessage() {
        ApiResponse<?> res = ApiResponse.ofError(401, "登录状态失效，请重新登录");
        assertEquals(401, res.getCode());
        assertEquals("登录状态失效，请重新登录", res.getMsg());
    }

    @Test
    @DisplayName("ofSuccess(data) 应携带数据")
    void shouldCarryDataOnSuccess() {
        ApiResponse<String> res = ApiResponse.ofSuccess("hello");
        assertEquals("hello", res.getData());
        assertEquals(0, res.getCode());
    }

    @Test
    @DisplayName("ofError(code, msg, data) 应同时携带错误信息和数据")
    void shouldCarryErrorData() {
        ApiResponse<String> res = ApiResponse.ofError(422, "验证失败", "field: email");
        assertEquals(422, res.getCode());
        assertEquals("验证失败", res.getMsg());
        assertEquals("field: email", res.getData());
    }
}
