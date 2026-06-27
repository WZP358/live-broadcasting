package cn.imhtb.live.modules.wallet.service.impl;

import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.mappers.WalletLogMapper;
import cn.imhtb.live.mappers.WalletMapper;
import cn.imhtb.live.modules.wallet.config.AlipayProperties;
import cn.imhtb.live.modules.wallet.model.RechargePayResp;
import cn.imhtb.live.modules.wallet.model.RechargeStatusResp;
import cn.imhtb.live.modules.wallet.service.AlipayRechargeService;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import cn.imhtb.live.pojo.database.Wallet;
import cn.imhtb.live.pojo.database.WalletLog;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements IWalletService {

    private static final String WALLET_TABLE = "tb_wallet";
    private static final String WALLET_LOG_TABLE = "tb_wallet_log";
    private static final String RECHARGE_ORDER_TABLE = "tb_recharge_order";
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
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_recharge_order` ("
                + "`id` int NOT NULL AUTO_INCREMENT,"
                + "`user_id` int NOT NULL,"
                + "`out_trade_no` varchar(64) NOT NULL,"
                + "`amount` decimal(16,2) NOT NULL,"
                + "`status` int NOT NULL DEFAULT '0',"
                + "`trade_status` varchar(64) DEFAULT NULL,"
                + "`pay_channel` varchar(32) NOT NULL DEFAULT 'alipay',"
                + "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "`paid_time` datetime DEFAULT NULL,"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uk_tb_recharge_order_no` (`out_trade_no`),"
                + "KEY `idx_tb_recharge_order_user` (`user_id`),"
                + "KEY `idx_tb_recharge_order_status` (`status`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        ensureColumn(WALLET_LOG_TABLE, "source_uuid", "ALTER TABLE `tb_wallet_log` ADD COLUMN `source_uuid` varchar(64) DEFAULT NULL");
        ensureColumn(WALLET_LOG_TABLE, "source_type", "ALTER TABLE `tb_wallet_log` ADD COLUMN `source_type` varchar(32) DEFAULT NULL");
        ensureIndex(WALLET_LOG_TABLE, "uk_tb_wallet_log_source_uuid", "ALTER TABLE `tb_wallet_log` ADD UNIQUE KEY `uk_tb_wallet_log_source_uuid` (`source_uuid`)");
        ensureIndex(WALLET_TABLE, "uk_tb_wallet_user_id", "ALTER TABLE `tb_wallet` ADD UNIQUE KEY `uk_tb_wallet_user_id` (`user_id`)");
        ensureIndex(RECHARGE_ORDER_TABLE, "uk_tb_recharge_order_no", "ALTER TABLE `tb_recharge_order` ADD UNIQUE KEY `uk_tb_recharge_order_no` (`out_trade_no`)");
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
    @Transactional(rollbackFor = Exception.class)
    public boolean decrease(Integer userId, BigDecimal fee) {
        ensureWalletTableAvailable();
        Wallet wallet = getWallet(userId);
        BigDecimal change = normalizeWalletChange(fee, "扣款金额");
        boolean updated = lambdaUpdate()
                .setSql("balance = balance - " + change.toPlainString())
                .setSql("version = version + 1")
                .eq(Wallet::getId, wallet.getId())
                .ge(Wallet::getBalance, change)
                .update();
        if (updated) {
            Wallet updatedWallet = getById(wallet.getId());
            insertWalletLog(wallet.getId(), updatedWallet.getBalance(), change.negate(), 2, null, "gift_spend");
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increase(Integer userId, BigDecimal fee) {
        ensureWalletTableAvailable();
        Wallet wallet = getWallet(userId);
        BigDecimal change = normalizeWalletChange(fee, "入账金额");
        boolean updated = lambdaUpdate()
                .setSql("balance = balance + " + change.toPlainString())
                .setSql("version = version + 1")
                .eq(Wallet::getId, wallet.getId())
                .update();
        if (updated) {
            Wallet updatedWallet = getById(wallet.getId());
            insertWalletLog(wallet.getId(), updatedWallet.getBalance(), change, 3, null, "gift_income");
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
        createRechargeOrder(userId, outTradeNo, rechargeCount);
        return RechargePayResp.builder()
                .outTradeNo(outTradeNo)
                .payHtml(payHtml)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeStatusResp queryAlipayRecharge(Integer userId, String outTradeNo) {
        ensureWalletTableAvailable();
        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException("支付宝订单号不能为空");
        }
        Integer orderUserId = parseUserId(outTradeNo);
        if (!orderUserId.equals(userId)) {
            throw new BusinessException("只能查询自己的充值订单");
        }

        Wallet wallet = getWallet(userId);
        WalletLog completedLog = getWalletLogBySourceUuid(outTradeNo);
        if (completedLog != null) {
            return RechargeStatusResp.builder()
                    .outTradeNo(outTradeNo)
                    .tradeStatus("LOCAL_COMPLETED")
                    .paid(true)
                    .amount(completedLog.getFee())
                    .balance(wallet.getBalance())
                    .message("充值已入账")
                    .build();
        }

        Map<String, Object> order = getRechargeOrder(outTradeNo);
        if (order == null || !userId.equals(((Number) order.get("user_id")).intValue())) {
            throw new BusinessException("充值订单不存在或不属于当前用户");
        }

        RechargeStatusResp status = alipayRechargeService.queryTrade(outTradeNo);
        if (!status.isPaid()) {
            status.setBalance(wallet.getBalance());
            return status;
        }

        BigDecimal amount = resolvePaidAmount(status.getAmount(), order);
        rechargeOrderWallet(userId, amount, outTradeNo, ALIPAY_SOURCE_TYPE, status.getTradeStatus());
        Wallet updatedWallet = getWallet(userId);
        status.setBalance(updatedWallet.getBalance());
        status.setAmount(amount);
        status.setMessage("充值已入账");
        return status;
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

        Map<String, Object> order = getRechargeOrder(outTradeNo);
        if (order == null) {
            log.warn("alipay notify order not found, outTradeNo = {}", outTradeNo);
            return false;
        }
        Integer userId = ((Number) order.get("user_id")).intValue();
        BigDecimal amount = resolvePaidAmount(normalizeRechargeAmount(params.get("total_amount")), order);
        return rechargeOrderWallet(userId, amount, outTradeNo, ALIPAY_SOURCE_TYPE, tradeStatus);
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

        Map<String, Object> order = getRechargeOrder(outTradeNo);
        if (order == null) {
            log.warn("alipay return order not found, outTradeNo = {}", outTradeNo);
            return false;
        }
        Integer userId = ((Number) order.get("user_id")).intValue();
        BigDecimal amount = resolvePaidAmount(normalizeRechargeAmount(params.get("total_amount")), order);
        return rechargeOrderWallet(userId, amount, outTradeNo, ALIPAY_SOURCE_TYPE, "SYNC_RETURN");
    }

    private void createRechargeOrder(Integer userId, String outTradeNo, BigDecimal amount) {
        jdbcTemplate.update(
                "INSERT INTO tb_recharge_order (user_id, out_trade_no, amount, status, trade_status, pay_channel) "
                        + "VALUES (?, ?, ?, 0, 'WAIT_BUYER_PAY', ?) "
                        + "ON DUPLICATE KEY UPDATE update_time = NOW()",
                userId,
                outTradeNo,
                amount,
                ALIPAY_SOURCE_TYPE
        );
    }

    private Map<String, Object> getRechargeOrder(String outTradeNo) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM tb_recharge_order WHERE out_trade_no = ? LIMIT 1",
                outTradeNo
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private BigDecimal resolvePaidAmount(BigDecimal paidAmount, Map<String, Object> order) {
        BigDecimal orderAmount = normalizeWalletChange((BigDecimal) order.get("amount"), "充值订单金额");
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return orderAmount;
        }
        BigDecimal normalizedPaidAmount = normalizeWalletChange(paidAmount, "支付宝支付金额");
        if (normalizedPaidAmount.compareTo(orderAmount) != 0) {
            throw new BusinessException("支付金额与充值订单金额不一致");
        }
        return normalizedPaidAmount;
    }

    private Boolean rechargeOrderWallet(Integer userId, BigDecimal rechargeCount, String outTradeNo, String sourceType, String tradeStatus) {
        Map<String, Object> order = getRechargeOrder(outTradeNo);
        if (order == null) {
            throw new BusinessException("充值订单不存在");
        }
        BigDecimal amount = resolvePaidAmount(rechargeCount, order);
        Boolean recharged = rechargeWallet(userId, amount, outTradeNo, sourceType);
        if (Boolean.TRUE.equals(recharged)) {
            jdbcTemplate.update(
                    "UPDATE tb_recharge_order "
                            + "SET status = 1, trade_status = ?, paid_time = COALESCE(paid_time, NOW()), update_time = NOW() "
                            + "WHERE out_trade_no = ?",
                    StringUtils.hasText(tradeStatus) ? tradeStatus : "TRADE_SUCCESS",
                    outTradeNo
            );
        }
        return recharged;
    }

    private Boolean rechargeWallet(Integer userId, BigDecimal rechargeCount, String sourceUuid, String sourceType) {
        if (StringUtils.hasText(sourceUuid) && walletLogExists(sourceUuid)) {
            log.info("wallet recharge source already completed, sourceUuid = {}", sourceUuid);
            return true;
        }
        Wallet wallet = getWallet(userId);
        BigDecimal change = normalizeWalletChange(rechargeCount, "充值金额");

        Wrapper<Wallet> wrapper = new LambdaUpdateWrapper<Wallet>()
                .eq(Wallet::getId, wallet.getId())
                .setSql("balance = balance + " + change.toPlainString())
                .setSql("version = version + 1");
        boolean update = update(wrapper);
        if (!update) {
            throw new BusinessException("钱包余额更新失败，请稍后重试");
        }

        Wallet updatedWallet = getById(wallet.getId());
        insertWalletLog(wallet.getId(), updatedWallet.getBalance(), change, 1, sourceUuid, sourceType);
        log.info("wallet recharge completed, userId={}, amount={}, balance={}, sourceType={}, sourceUuid={}",
                userId, change, updatedWallet.getBalance(), sourceType, sourceUuid);
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
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE UPPER(TABLE_NAME) = UPPER(?)",
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
                throw new BusinessException("充值金额必须大于 0");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new BusinessException("充值金额格式不正确");
        }
    }

    private BigDecimal normalizeWalletChange(BigDecimal fee, String name) {
        if (fee == null) {
            throw new BusinessException(name + "不能为空");
        }
        BigDecimal amount = fee.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(name + "必须大于 0");
        }
        return amount;
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
        if (!StringUtils.hasText(sourceUuid)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_wallet_log WHERE source_uuid = ?",
                Integer.class,
                sourceUuid
        );
        return count != null && count > 0;
    }

    private WalletLog getWalletLogBySourceUuid(String sourceUuid) {
        if (!StringUtils.hasText(sourceUuid)) {
            return null;
        }
        return walletLogMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WalletLog>()
                .eq(WalletLog::getSourceUuid, sourceUuid)
                .last("limit 1"));
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            return indexExists(metaData, connection.getCatalog(), tableName, indexName)
                    || indexExists(metaData, null, tableName, indexName);
        });
        if (!Boolean.TRUE.equals(exists)) {
            try {
                jdbcTemplate.execute(ddl);
            } catch (DataAccessException e) {
                if (!isDuplicateIndexException(e, indexName)) {
                    throw e;
                }
                log.info("wallet schema index already exists, table={}, index={}", tableName, indexName);
            }
        }
    }

    private boolean indexExists(DatabaseMetaData metaData, String catalog, String tableName, String indexName) throws SQLException {
        String[] tableCandidates = {
                tableName,
                tableName.toUpperCase(Locale.ROOT),
                tableName.toLowerCase(Locale.ROOT)
        };
        for (String tableCandidate : tableCandidates) {
            try (ResultSet indexes = metaData.getIndexInfo(catalog, null, tableCandidate, false, false)) {
                while (indexes.next()) {
                    String foundIndexName = indexes.getString("INDEX_NAME");
                    if (foundIndexName != null && foundIndexName.equalsIgnoreCase(indexName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isDuplicateIndexException(DataAccessException e, String indexName) {
        String expectedIndex = indexName.toUpperCase(Locale.ROOT);
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toUpperCase(Locale.ROOT);
                boolean duplicate = normalized.contains("DUPLICATE") || normalized.contains("ALREADY EXISTS");
                if (duplicate && normalized.contains(expectedIndex)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
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
