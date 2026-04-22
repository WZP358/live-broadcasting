package cn.imhtb.live.modules.wallet.service.impl;

import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.mappers.WalletLogMapper;
import cn.imhtb.live.mappers.WalletMapper;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import cn.imhtb.live.pojo.database.Wallet;
import cn.imhtb.live.pojo.database.WalletLog;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Service
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements IWalletService {

    private static final String WALLET_TABLE = "tb_wallet";
    private static final String WALLET_TABLE_MISSING_MESSAGE = "当前环境未初始化钱包表，暂不支持充值或送礼功能";

    @Resource
    private WalletLogMapper walletLogMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public Wallet getWallet(Integer userId) {
        if (!walletTableExists()) {
            log.warn("wallet table is unavailable in current schema, returning synthetic wallet, userId = {}", userId);
            return buildSyntheticWallet(userId);
        }

        Wallet wallet = lambdaQuery().eq(Wallet::getUserId, userId).one();
        if (wallet == null) {
            return delayInitUserWallet(userId);
        }
        return wallet;
    }

    @Override
    public boolean decrease(Integer userId, BigDecimal fee) {
        ensureWalletTableAvailable();
        Wallet wallet = getWallet(userId);
        return lambdaUpdate()
                .setSql("balance = balance - " + fee)
                .eq(Wallet::getBalance, wallet.getBalance())
                .eq(Wallet::getId, wallet.getId())
                .update();
    }

    @Override
    public boolean increase(Integer userId, BigDecimal fee) {
        ensureWalletTableAvailable();
        Wallet wallet = getWallet(userId);
        return lambdaUpdate()
                .setSql("balance = balance + " + fee)
                .eq(Wallet::getBalance, wallet.getBalance())
                .eq(Wallet::getId, wallet.getId())
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rechargeMock(Integer userId, String count) {
        ensureWalletTableAvailable();

        Wallet wallet = getWallet(userId);
        BigDecimal rechargeCount = new BigDecimal(count);
        BigDecimal newCount = wallet.getBalance().add(rechargeCount);

        Wrapper<Wallet> wrapper = new LambdaUpdateWrapper<Wallet>()
                .eq(Wallet::getId, wallet.getId())
                .eq(Wallet::getVersion, wallet.getVersion())
                .set(Wallet::getBalance, newCount)
                .set(Wallet::getVersion, wallet.getVersion() + 1);

        boolean update = update(wrapper);
        if (update) {
            WalletLog walletLog = new WalletLog();
            walletLog.setFee(rechargeCount);
            walletLog.setBalance(newCount);
            walletLog.setWalletId(wallet.getId());
            walletLog.setActionType(1);
            walletLogMapper.insert(walletLog);
            return true;
        }
        return false;
    }

    private Wallet delayInitUserWallet(Integer userId) {
        try {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setStatus(0);
            wallet.setVersion(0);
            save(wallet);
            return wallet;
        } catch (Exception e) {
            log.warn("init user wallet error, userId = {}", userId, e);
            return getWallet(userId);
        }
    }

    private void ensureWalletTableAvailable() {
        if (!walletTableExists()) {
            throw new BusinessException(WALLET_TABLE_MISSING_MESSAGE);
        }
    }

    private boolean walletTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                WALLET_TABLE
        );
        return count != null && count > 0;
    }

    private Wallet buildSyntheticWallet(Integer userId) {
        Wallet wallet = new Wallet();
        wallet.setId(0);
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setVersion(0);
        wallet.setStatus(0);
        return wallet;
    }
}
