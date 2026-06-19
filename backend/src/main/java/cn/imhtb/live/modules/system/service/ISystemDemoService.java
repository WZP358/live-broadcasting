package cn.imhtb.live.modules.system.service;

import cn.imhtb.live.modules.system.model.SystemDemoStatus;

public interface ISystemDemoService {

    SystemDemoStatus status();

    SystemDemoStatus enable();

    SystemDemoStatus disable();
}
