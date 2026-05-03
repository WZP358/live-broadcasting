package cn.imhtb.live.modules.system.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.mappers.BillMapper;
import cn.imhtb.live.pojo.database.Bill;
import cn.imhtb.live.pojo.vo.request.IdsRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/system/bill")
public class SystemBillController {

    private final BillMapper billMapper;

    public SystemBillController(BillMapper billMapper) {
        this.billMapper = billMapper;
    }

    @GetMapping("/page")
    public ApiResponse<PageData<Bill>> page(@RequestParam(defaultValue = "1") Integer pageNo,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) Integer userId,
                                            @RequestParam(required = false) Integer type,
                                            @RequestParam(required = false) String mark) {
        Page<Bill> page = billMapper.selectPage(new Page<>(pageNo, pageSize), new QueryWrapper<Bill>()
                .eq(userId != null, "user_id", userId)
                .eq(type != null, "type", type)
                .eq(StringUtils.hasText(mark), "mark", mark)
                .orderByDesc("id"));
        return ApiResponse.ofSuccess(new PageData<>(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/save")
    public ApiResponse<Boolean> save(@RequestBody Bill bill) {
        if (bill.getId() == null) {
            return ApiResponse.ofSuccess(billMapper.insert(bill) > 0);
        }
        return ApiResponse.ofSuccess(billMapper.updateById(bill) > 0);
    }

    @PostMapping("/delete")
    public ApiResponse<Boolean> delete(@RequestBody IdsRequest request) {
        return ApiResponse.ofSuccess(billMapper.deleteBatchIds(Arrays.asList(request.getIds())) > 0);
    }
}
