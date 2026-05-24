package com.recruit.airecruitsystem.service.common;

public interface EmailService {

    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱（用户的邮箱）
     * @param type 验证码类型（seeker_reset / hr_reset）
     * @return 错误码（0-成功，其他失败）
     */
    int sendVerificationCode(String toEmail, String type);
}
