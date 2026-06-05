package com.meeting;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class VerificationCodeService {

    // 存储验证码（邮箱 -> 验证码）
    private Map<String, String> codeMap = new HashMap<>();
    private Map<String, Long> expireMap = new HashMap<>();
    private static final long EXPIRE_TIME = 5 * 60 * 1000; // 5分钟过期

    // 生成6位随机验证码
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    // 保存验证码
    public void saveCode(String email, String code) {
        codeMap.put(email, code);
        expireMap.put(email, System.currentTimeMillis() + EXPIRE_TIME);
    }

    // 验证验证码
    public boolean verifyCode(String email, String code) {
        Long expireTime = expireMap.get(email);
        if (expireTime == null || System.currentTimeMillis() > expireTime) {
            return false; // 验证码不存在或已过期
        }
        String savedCode = codeMap.get(email);
        return savedCode != null && savedCode.equals(code);
    }

    // 清除验证码
    public void clearCode(String email) {
        codeMap.remove(email);
        expireMap.remove(email);
    }
}