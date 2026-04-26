package cn.imhtb.live.common.exception;

import cn.imhtb.live.common.exception.base.IErrorCode;
import cn.imhtb.live.common.exception.base.CommonErrorCode;

/**
 * @author pinteh
 * @date 2023/2/28
 */
public class BusinessException extends RuntimeException implements IErrorCode {

    private final int code;

    private final String msg;


    public BusinessException(IErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public BusinessException(IErrorCode errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
        this.msg = msg;
    }

    public BusinessException(String msg){
        super(msg);
        this.code = CommonErrorCode.SERVICE_ERROR.getCode();
        this.msg = msg;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = CommonErrorCode.SERVICE_ERROR.getCode();
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
