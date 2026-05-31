package cn.imhtb.live.modules.live.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.modules.live.vo.SearchResultVO;

public interface ISearchService {
    PageData<SearchResultVO> searchRooms(String keyword, Integer page, Integer limit);
}
