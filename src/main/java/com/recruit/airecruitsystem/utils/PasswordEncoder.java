package com.recruit.airecruitsystem.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {

    // 加密：使用 BCrypt 哈希，自动生成随机盐
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    // 验证：明文密码与哈希是否匹配
    public boolean matches(String rawPassword, String encodedPassword) {
        System.out.println("---------------------------------");
        System.out.println(rawPassword);
        System.out.println(encodedPassword);
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}