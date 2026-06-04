package cn.imhtb.live.modules.wallet.service.impl;

import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.mappers.WalletLogMapper;
import cn.imhtb.live.mappers.WalletMapper;
import cn.imhtb.live.modules.wallet.config.AlipayProperties;
import cn.imhtb.live.modules.wallet.model.RechargePayResp;
import cn.imhtb.live.modules.wallet.service.AlipayRechargeService;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import cn.imhtb.live.pojo.database.Wallet;
import cn.imhtb.live.pojo.database.WalletLog;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements IWalletService {

    private static final String WALLET_TABLE = "tb_wallet";
    private static final String WALLET_LOG_TABLE = "tb_wallet_log";
    private static final String ALIPAY_SOURCE_TYPE = "alipay";
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private WalletLogMapper walletLogMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private AlipayRechargeService alipayRechargeService;
    @Resource
    private AlipayProperties alipayProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void initWalletSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_wallet` ("
                + "`id` int NOT NULL AUTO_INCREMENT,"
                + "`user_id` int NOT NULL,"
                + "`balance` decimal(16,2) NOT NULL DEFAULT '0.00',"
                + "`version` int NOT NULL DEFAULT '0',"
                + "`sign` varchar(255) DEFAULT NULL,"
                + "`status` int NOT NULL DEFAULT '0',"
                + "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uk_tb_wallet_user_id` (`user_id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_wallet_log` ("
                + "`id` int NOT NULL AUTO_INCREMENT,"
                + "`wallet_id` int NOT NULL,"
                + "`balance` decimal(16,2) NOT NULL,"
                + "`fee` decimal(16,2) NOT NULL,"
                + "`action_type` int NOT NULL,"
                + "`source_uuid` varchar(64) DEFAULT NULL,"
                + "`source_type` varchar(32) DEFAULT NULL,"
                + "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uk_tb_wallet_log_source_uuid` (`source_uuid`),"
                + "KEY `idx_tb_wallet_log_wallet_id` (`wallet_id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        ensureColumn(WALLET_LOG_TABLE, "source_uuid", "ALTER TABLE `tb_wallet_log` ADD COLUMN `source_uuid` varchar(64) DEFAULT NULL");
        ensureColumn(WALLET_LOG_TABLE, "source_type", "ALTER TABLE `tb_wallet_log` ADD COLUMN `source_type` varchar(32) DEFAULT NULL");
        ensureIndex(WALLET_LOG_TABLE, "uk_tb_wallet_log_source_uuid", "ALTER TABLE `tb_wallet_log` ADD UNIQUE KEY `uk_tb_wallet_log_source_uuid` (`source_uuid`)");
        ensureIndex(WALLET_TABLE, "uk_tb_wallet_user_id", "ALTER TABLE `tb_wallet` ADD UNIQUE KEY `uk_tb_wallet_user_id` (`user_id`)");
    }

    @Override
    public Wallet getWallet(Integer userId) {
        if (!walletTableExists()) {
            initWalletSchema();
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
        BigDecimal change = fee.setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = wallet.getBalance().subtract(change).setScale(2, RoundingMode.HALF_UP);
        boolean updated = lambdaUpdate()
                .setSql("balance = balance - " + fee)
                .eq(Wallet::getBalance, wallet.getBalance())
                .eq(Wallet::getId, wallet.getId())
                .update();
        if (updated) {
            insertWalletLog(wallet.getId(), newBalance, change.negate(), 2, null, "gift_spend");
        }
        return updated;
    }

    @Override
    public boolean increase(Integer userId, BigDecimal fee) {
        ensureWalletTableAvailable();
        Wallet wallet = getWallet(userId);
        BigDecimal change = fee.setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = wallet.getBalance().add(change).setScale(2, RoundingMode.HALF_UP);
        boolean updated = lambdaUpdate()
                .setSql("balance = balance + " + fee)
                .eq(Wallet::getBalance, wallet.getBalance())
                .eq(Wallet::getId, wallet.getId())
                .update();
        if (updated) {
            insertWalletLog(wallet.getId(), newBalance, change, 3, null, "gift_income");
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rechargeMock(Integer userId, String count) {
        ensureWalletTableAvailable();
        return rechargeWallet(userId, normalizeRechargeAmount(count), null, "mock");
    }

    @Override
    public RechargePayResp createAlipayRecharge(Integer userId, String count) {
        ensureWalletTableAvailable();
        BigDecimal rechargeCount = normalizeRechargeAmount(count);
        String outTradeNo = buildOutTradeNo(userId);
        String subject = alipayProperties.getSubjectPrefix() + " " + rechargeCount.toPlainString();
        String payHtml = alipayRechargeService.createPagePay(outTradeNo, rechargeCount, subject);
        return RechargePayResp.builder()
                .outTradeNo(outTradeNo)
                .payHtml(payHtml)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAlipayRecharge(Map<String, String> params) {
        log.info("receive alipay notify, outTradeNo={}, tradeStatus={}, totalAmount={}",
                params.get("out_trade_no"), params.get("trade_status"), params.get("total_amount"));
        if (!alipayRechargeService.verify(params)) {
            log.warn("alipay notify signature verification failed, outTradeNo = {}", params.get("out_trade_no"));
            return false;
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("ignore alipay notify with status = {}, outTradeNo = {}", tradeStatus, params.get("out_trade_no"));
            return true;
        }

        String outTradeNo = params.get("out_trade_no");
        if (!StringUtils.hasText(outTradeNo)) {
            log.warn("alipay notify missing out_trade_no");
            return false;
        }
        if (walletLogExists(outTradeNo)) {
            log.info("alipay recharge already completed, outTradeNo = {}", outTradeNo);
            return true;
        }

        Integer userId = parseUserId(outTradeNo);
        BigDecimal amount = normalizeRechargeAmount(params.get("total_amount"));
        return rechargeWallet(userId, amount, outTradeNo, ALIPAY_SOURCE_TYPE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAlipayReturn(Map<String, String> params) {
        log.info("receive alipay return, outTradeNo={}, totalAmount={}",
                params.get("out_trade_no"), params.get("total_amount"));
        if (!alipayRechargeService.verify(params)) {
            log.warn("alipay return signature verification failed, outTradeNo = {}", params.get("out_trade_no"));
            return false;
        }

        String outTradeNo = params.get("out_trade_no");
        if (!StringUtils.hasText(outTradeNo)) {
            log.warn("alipay return missing out_trade_no");
            return false;
        }
        if (walletLogExists(outTradeNo)) {
            log.info("alipay recharge already completed by return, outTradeNo = {}", outTradeNo);
            return true;
        }

        Integer userId = parseUserId(outTradeNo);
        BigDecimal amount = normalizeRechargeAmount(params.get("total_amount"));
        return rechargeWallet(userId, amount, outTradeNo, ALIPAY_SOURCE_TYPE);
    }

    private Boolean rechargeWallet(Integer userId, BigDecimal rechargeCount, String sourceUuid, String sourceType) {
        Wallet wallet = getWallet(userId);
        BigDecimal newCount = wallet.getBalance().add(rechargeCount).setScale(2, RoundingMode.HALF_UP);

        Wrapper<Wallet> wrapper = new LambdaUpdateWrapper<Wallet>()
                .eq(Wallet::getId, wallet.getId())
                .eq(Wallet::getVersion, wallet.getVersion())
                .set(Wallet::getBalance, newCount)
                .set(Wallet::getVersion, wallet.getVersion() + 1);
        boolean update = update(wrapper);
        if (!update) {
            throw new BusinessException("钱包余额更新失败，请稍后重试");
        }

        insertWalletLog(wallet.getId(), newCount, rechargeCount, 1, sourceUuid, sourceType);
        log.info("wallet recharge completed, userId={}, amount={}, balance={}, sourceType={}, sourceUuid={}",
                userId, rechargeCount, newCount, sourceType, sourceUuid);
        return true;
    }

    private void insertWalletLog(Integer walletId, BigDecimal balance, BigDecimal fee, Integer actionType, String sourceUuid, String sourceType) {
        WalletLog walletLog = new WalletLog();
        walletLog.setWalletId(walletId);
        walletLog.setBalance(balance);
        walletLog.setFee(fee);
        walletLog.setActionType(actionType);
        walletLog.setSourceUuid(sourceUuid);
        walletLog.setSourceType(sourceType);
        walletLogMapper.insert(walletLog);
    }

    private Wallet delayInitUserWallet(Integer userId) {
        jdbcTemplate.update(
                "INSERT INTO tb_wallet (user_id, balance, version, status) VALUES (?, 0.00, 0, 0) "
                        + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)",
                userId
        );
        Wallet wallet = lambdaQuery().eq(Wallet::getUserId, userId).last("LIMIT 1").one();
        if (wallet == null) {
            throw new BusinessException("钱包初始化失败，请稍后重试");
        }
        return wallet;
    }

    private void ensureWalletTableAvailable() {
        if (!walletTableExists()) {
            initWalletSchema();
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

    private BigDecimal normalizeRechargeAmount(String count) {
        if (!StringUtils.hasText(count)) {
            throw new BusinessException("充值金额不能为空");
        }
        try {
            BigDecimal amount = new BigDecimal(count).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("充值金额必须大于0");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new BusinessException("充值金额格式不正确");
        }
    }

    private String buildOutTradeNo(Integer userId) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ALR_" + userId + "_" + LocalDateTime.now().format(ORDER_TIME_FORMATTER) + "_" + random;
    }

    private Integer parseUserId(String outTradeNo) {
        String[] parts = outTradeNo.split("_");
        if (parts.length < 2 || !"ALR".equals(parts[0])) {
            throw new BusinessException("支付宝订单号格式不正确");
        }
        try {
            return Integer.valueOf(parts[1]);
        } catch (NumberFormatException e) {
            throw new BusinessException("支付宝订单号用户标识不正确");
        }
    }

    private boolean walletLogExists(String sourceUuid) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_wallet_log WHERE source_uuid = ?",
                Integer.class,
                sourceUuid
        );
        return count != null && count > 0;
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    @Override
    public List<Map<String, Object>> getRechargeTiers() {
        List<Map<String, Object>> tiers = new ArrayList<>();
        int[] amounts = {6, 10, 50, 100, 128, 256, 328, 648};
        for (int i = 0; i < amounts.length; i++) {
            Map<String, Object> tier = new LinkedHashMap<>();
            tier.put("id", i + 1);
            tier.put("value", amounts[i]);
            tier.put("fee", String.format("%.2f", (double) amounts[i]));
            tiers.add(tier);
        }
        return tiers;
    }
}
