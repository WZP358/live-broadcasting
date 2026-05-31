package cn.imhtb.live.modules.live.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.modules.live.service.ISearchService;
import cn.imhtb.live.modules.live.vo.SearchResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "搜索接口")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SearchController {

    private final ISearchService searchService;

    @ApiOperation("全局搜索直播间和主播")
    @GetMapping("/rooms")
    public ApiResponse<PageData<SearchResultVO>> searchRooms(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer limit) {
        return ApiResponse.ofSuccess(searchService.searchRooms(keyword, page, limit));
    }
}
