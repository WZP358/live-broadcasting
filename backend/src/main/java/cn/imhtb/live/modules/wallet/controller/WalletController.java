package cn.imhtb.live.modules.wallet.controller;

import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.PageData;
import cn.imhtb.live.common.annotation.IgnoreToken;
import cn.imhtb.live.common.holder.UserHolder;
import cn.imhtb.live.modules.wallet.config.AlipayProperties;
import cn.imhtb.live.modules.wallet.model.RechargePayResp;
import cn.imhtb.live.modules.wallet.model.RechargeReq;
import cn.imhtb.live.modules.wallet.model.WalletLogResp;
import cn.imhtb.live.modules.wallet.service.IWalletLogService;
import cn.imhtb.live.modules.wallet.service.IWalletService;
import cn.imhtb.live.pojo.database.Wallet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "钱包接口")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final IWalletService walletService;
    private final IWalletLogService walletLogService;
    private final AlipayProperties alipayProperties;

    @ApiOperation("获取钱包")
    @GetMapping("/getBalance")
    public ApiResponse<Wallet> getBalance() {
        Wallet wallet = walletService.getWallet(UserHolder.getUserId());
        return ApiResponse.ofSuccess(wallet);
    }

    @ApiOperation("获取钱包最近的变化记录")
    @GetMapping("/listRecentWalletLogs")
    public ApiResponse<?> listRecentWalletLogs() {
        PageData<WalletLogResp> pageData = walletLogService.listRecentWalletLogs(UserHolder.getUserId());
        return ApiResponse.ofSuccess(pageData);
    }

    @ApiOperation("获取钱包变化记录")
    @GetMapping("/listWalletLogs")
    public ApiResponse<PageData<WalletLogResp>> listWalletLogs(@RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        PageData<WalletLogResp> pageData = walletLogService.listWalletLogs(UserHolder.getUserId(), pageNo, pageSize);
        return ApiResponse.ofSuccess(pageData);
    }

    @ApiOperation("创建支付宝沙箱充值订单")
    @PostMapping("/recharge")
    public ApiResponse<RechargePayResp> recharge(@RequestBody RechargeReq rechargeReq) {
        RechargePayResp resp = walletService.createAlipayRecharge(UserHolder.getUserId(), rechargeReq.getFee());
        return ApiResponse.ofSuccess(resp);
    }

    @IgnoreToken
    @ApiOperation("支付宝异步通知")
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = getRequestParams(request);
        log.info("alipay notify request received, outTradeNo={}", params.get("out_trade_no"));
        boolean success = walletService.completeAlipayRecharge(params);
        return success ? "success" : "failure";
    }

    @IgnoreToken
    @ApiOperation("支付宝同步返回")
    @GetMapping("/alipay/return")
    public RedirectView alipayReturn(HttpServletRequest request) {
        Map<String, String> params = getRequestParams(request);
        log.info("alipay return request received, outTradeNo={}", params.get("out_trade_no"));
        walletService.completeAlipayReturn(params);
        return new RedirectView(alipayProperties.getReturnUrl());
    }

    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
