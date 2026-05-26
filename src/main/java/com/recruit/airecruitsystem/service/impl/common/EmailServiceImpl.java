package com.recruit.airecruitsystem.service.impl.common;


import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.mapper.EmailVerifyCodeMapper;
import com.recruit.airecruitsystem.pojo.EmailVerifyCode;
import com.recruit.airecruitsystem.service.common.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {


    @Autowired
    private JavaMailSender mailSender;          // Spring 自动配置的邮件发送器

    @Autowired
    private TemplateEngine templateEngine;      // Thymeleaf 模板引擎，用于渲染 HTML 邮件

    @Autowired
    private EmailVerifyCodeMapper emailVerifyCodeMapper;

    @Value("${spring.mail.username}")           // 从配置文件读取发件人邮箱（你自己的）
    private String fromEmail;

    @Override
    public int sendVerificationCode(String toEmail, String type) {
        // 1. 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        try {
            // 2. 创建 MimeMessage 并设置 UTF-8 编码
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 3. 设置邮件头信息
            helper.setFrom(fromEmail);               // 发件人（你自己的 QQ 邮箱）
            helper.setTo(toEmail);                   // 收件人（用户填写的邮箱）
            helper.setSubject("【AI招聘猎头系统】密码重置验证码");

            // 4. 使用 Thymeleaf 模板渲染 HTML 内容
            Context context = new Context();
            context.setVariable("code", code);       // 将验证码传入模板
            String emailContent = templateEngine.process("email-verification-code", context);
            helper.setText(emailContent, true);      // true 表示 HTML 格式

            // 5. 发送邮件
            mailSender.send(mimeMessage);
            log.info("验证码邮件发送成功，目标: {}，验证码: {}", toEmail, code);

            // 6. 邮件发送成功后，将验证码存入数据库（有效期5分钟）
            EmailVerifyCode record = EmailVerifyCode.builder()
                    .email(toEmail)
                    .code(code)
                    .type(type)
                    .expireTime(LocalDateTime.now().plusMinutes(5))
                    .build();
            emailVerifyCodeMapper.insert(record);

            return ResultCode.SUCCESS;
        } catch (MessagingException e) {
            log.error("验证码邮件发送失败，目标: {}，错误: {}", toEmail, e.getMessage());
            return ResultCode.PARAM_ERROR;
        }
    }
}
