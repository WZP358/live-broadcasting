package cn.imhtb.live.modules.wallet.service.impl;

import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.utils.CovertBeanUtil;
import cn.imhtb.live.mappers.WalletLogMapper;
import cn.imhtb.live.modules.wallet.model.WalletLogResp;
import cn.imhtb.live.modules.wallet.service.IWalletLogService;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import cn.imhtb.live.pojo.database.Wallet;
import cn.imhtb.live.pojo.database.WalletLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * @author pinteh
 * @date 2025/4/2
 */
@Slf4j
@Service
public class WalletLogServiceImpl extends ServiceImpl<WalletLogMapper, WalletLog> implements IWalletLogService {

    private static final String WALLET_LOG_TABLE = "tb_wallet_log";

    @Resource
    private IWalletService walletService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public PageData<WalletLogResp> listRecentWalletLogs(Integer userId) {
        return this.listWalletLogs(userId, 1, 10);
    }

    @Override
    public PageData<WalletLogResp> listWalletLogs(Integer userId, Integer pageNo, Integer pageSize) {
        if (!walletLogTableExists()) {
            log.warn("wallet log table is unavailable in current schema, returning empty page, userId = {}", userId);
            return emptyPage();
        }

        Wallet wallet = walletService.getWallet(userId);
        if (wallet == null || wallet.getId() == null || wallet.getId() <= 0) {
            return emptyPage();
        }

        Page<WalletLog> page = lambdaQuery()
                .eq(WalletLog::getWalletId, wallet.getId())
                .orderByDesc(WalletLog::getCreateTime)
                .page(new Page<>(pageNo, pageSize));

        PageData<WalletLogResp> ans = new PageData<>();
        ans.setTotal(page.getTotal());
        ans.setList(CovertBeanUtil.covertList(page.getRecords(), WalletLogResp.class, (s, t) -> {
            String actionName = resolveActionTypeName(s);
            t.setActionTypeName(actionName);
            t.setRemark(actionName);
        }));
        return ans;
    }

    private String resolveActionTypeName(WalletLog log) {
        if (log == null || log.getActionType() == null) {
            return "钱包变动";
        }
        if (log.getActionType() == 1) {
            return "充值到账";
        }
        if (log.getActionType() == 2) {
            return "送礼消费";
        }
        if (log.getActionType() == 3) {
            return "礼物收入";
        }
        return "钱包变动";
    }

    private boolean walletLogTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                WALLET_LOG_TABLE
        );
        return count != null && count > 0;
    }

    private PageData<WalletLogResp> emptyPage() {
        PageData<WalletLogResp> empty = new PageData<>();
        empty.setTotal(0L);
        empty.setList(Collections.emptyList());
        return empty;
    }
}
