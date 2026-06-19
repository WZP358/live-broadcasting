package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.pojo.database.Report;
import cn.imhtb.live.service.IReportService;
import cn.imhtb.live.common.PageData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportController 举报接口")
class ReportControllerTest {

    private IReportService reportService;
    private ReportController controller;

    @BeforeEach
    void setUp() {
        reportService = mock(IReportService.class);
        controller = new ReportController(reportService);
        UserHolder.setUserId(999);
    }

    @AfterEach
    void tearDown() {
        UserHolder.remove();
    }

    @Test
    @DisplayName("提交举报成功")
    void shouldSubmitReportSuccessfully() {
        when(reportService.submitReport(any(Report.class))).thenReturn(true);

        ReportController.SubmitRequest req = new ReportController.SubmitRequest();
        req.setTargetUserId(100);
        req.setRoomId(1);
        req.setTargetType("room");
        req.setTargetId("1");
        req.setReason("违规内容");
        req.setDescription("测试举报");

        ApiResponse<Boolean> res = controller.submit(req);

        assertTrue(res.isSuccess());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportService).submitReport(captor.capture());
        Report saved = captor.getValue();
        assertEquals(999, saved.getReporterId());
        assertEquals(100, saved.getTargetUserId());
        assertEquals("违规内容", saved.getReason());
        assertEquals(0, saved.getStatus());
    }

    @Test
    @DisplayName("提交举报失败返回错误消息")
    void shouldReturnErrorWhenSaveFails() {
        when(reportService.submitReport(any(Report.class))).thenReturn(false);

        ReportController.SubmitRequest req = new ReportController.SubmitRequest();
        req.setReason("欺诈诈骗");

        ApiResponse<Boolean> res = controller.submit(req);

        assertEquals(1, res.getCode());
        assertEquals("提交失败", res.getMsg());
    }

    @Test
    @DisplayName("获取我的举报列表")
    void shouldReturnMyReports() {
        when(reportService.listByUser(eq(999), anyInt(), anyInt()))
                .thenReturn(new PageData<>(0, java.util.Collections.emptyList()));

        ApiResponse<?> res = controller.myReports(1, 10);
        assertTrue(res.isSuccess());
    }
}
