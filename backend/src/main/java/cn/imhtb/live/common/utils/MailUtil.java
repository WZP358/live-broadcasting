package cn.imhtb.live.common.utils;

import cn.imhtb.live.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailUtil {

    @Value("${spring.mail.username:}")
    private String username;

    private final JavaMailSender mailSender;

    private final Logger logger = LoggerFactory.getLogger(MailUtil.class);

    public MailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleMessage(String email, String subject, String content) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("邮件发送账号未配置");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(username);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
            logger.info("email sent successfully, target={}", email);
        } catch (Exception e) {
            logger.error("email send failed, target={}", email, e);
            throw new BusinessException("邮件发送失败，请检查邮箱地址或SMTP配置");
        }
    }
}
