package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.live.guard.GuardCheckResult;
import cn.imhtb.live.modules.live.guard.LiveGuardService;
import cn.imhtb.live.modules.live.service.ILiveInfoService;
import cn.imhtb.live.modules.live.service.ILiveReplayService;
import cn.imhtb.live.modules.live.service.ILiveService;
import cn.imhtb.live.modules.live.vo.LiveInfoReqVo;
import cn.imhtb.live.modules.live.vo.StopLiveStatsVo;
import cn.imhtb.live.pojo.LiveStatusVo;
import cn.imhtb.live.pojo.StartOpenLiveVo;
import cn.imhtb.live.pojo.database.LiveInfo;
import cn.imhtb.live.pojo.database.LiveReplay;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "直播接口")
@RestController
@RequestMapping("/api/v1/live")
public class AntLiveController {

    @Autowired
    @Qualifier("LalLiveService")
    private ILiveService liveService;

    @Autowired
    private ILiveInfoService liveInfoService;

    @Autowired
    private LiveGuardService liveGuardService;

    @Autowired
    private ILiveReplayService liveReplayService;

    @ApiOperation("申请直播密钥")
    @PostMapping("/applySecret")
    public ApiResponse<StartOpenLiveVo> applySecret() {
        return ApiResponse.ofSuccess(liveService.applySecret());
    }

    @ApiOperation("停止直播")
    @PostMapping("/stopLive")
    public ApiResponse<StopLiveStatsVo> stopLive() {
        return ApiResponse.ofSuccess(liveService.stopLive());
    }

    @ApiOperation("获取直播状态")
    @GetMapping("/getLiveStatus")
    public ApiResponse<LiveStatusVo> getLiveStatus() {
        return ApiResponse.ofSuccess(liveService.getLiveStatus());
    }

    @ApiOperation("直播违规检测")
    @PostMapping("/guard/check")
    public ApiResponse<GuardCheckResult> guardCheck(@RequestParam Integer roomId,
                                                    @RequestParam("file") MultipartFile file) {
        return ApiResponse.ofSuccess(liveGuardService.checkFrame(roomId, file));
    }

    @ApiOperation("上传浏览器直播录像")
    @PostMapping("/record/upload")
    public ApiResponse<LiveReplay> uploadLiveRecord(@RequestParam Integer roomId,
                                                    @RequestParam(required = false) Long duration,
                                                    @RequestParam("file") MultipartFile file) {
        return ApiResponse.ofSuccess(liveReplayService.completeBrowserRecording(roomId, UserHolder.getUserId(), file, duration));
    }

    @ApiOperation("获取直播记录")
    @PostMapping("/getLiveRecords")
    public ApiResponse<PageData<LiveInfo>> getLiveRecords(@RequestBody LiveInfoReqVo req) {
        Page<LiveInfo> page = liveInfoService.page(new Page<>(req.getPageNo(), req.getPageSize()),
                new LambdaQueryWrapper<LiveInfo>()
                        .eq(LiveInfo::getUserId, UserHolder.getUserId())
                        .orderByDesc(LiveInfo::getCreateTime));
        PageData<LiveInfo> pageData = new PageData<>();
        pageData.setTotal(page.getTotal());
        pageData.setList(page.getRecords());
        return ApiResponse.ofSuccess(pageData);
    }
}
