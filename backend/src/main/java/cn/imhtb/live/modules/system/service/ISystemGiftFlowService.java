package cn.imhtb.live.modules.system.service;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.modules.system.model.SystemGiftFlowQuery;
import cn.imhtb.live.modules.system.model.SystemGiftFlowRecord;
import cn.imhtb.live.modules.system.model.SystemGiftFlowSummary;

public interface ISystemGiftFlowService {

    PageData<SystemGiftFlowRecord> page(SystemGiftFlowQuery query, Integer pageNo, Integer pageSize);

    SystemGiftFlowSummary summary(SystemGiftFlowQuery query);

}
