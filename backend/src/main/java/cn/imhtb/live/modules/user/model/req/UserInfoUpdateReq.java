package cn.imhtb.live.modules.user.model.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

/**
 * 用户基础资料更新请求。
 */
@Data
public class UserInfoUpdateReq {

    @JsonAlias("nickname")
    @NotBlank(message = "昵称不能为空")
    @Length(min = 1, max = 16, message = "昵称长度限制 1 到 16 个字符")
    private String nickName;

    @Length(max = 64, message = "个性签名最多 64 个字符")
    private String signature;

}
