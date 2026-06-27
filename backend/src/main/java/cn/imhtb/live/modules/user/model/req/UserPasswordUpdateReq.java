package cn.imhtb.live.modules.user.model.req;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
public class UserPasswordUpdateReq {

    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Length(min = 6, max = 24, message = "新密码长度限制 6 到 24 个字符")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Length(min = 6, max = 24, message = "确认密码长度限制 6 到 24 个字符")
    private String confirmPassword;

}
