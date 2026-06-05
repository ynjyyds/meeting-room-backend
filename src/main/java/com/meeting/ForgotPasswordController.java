package com.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/password")
@CrossOrigin(origins = "*")
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    // 第一步：发送验证码
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String email = body.get("email");

        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "请输入邮箱");
            return response;
        }

        // 检查邮箱是否存在
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "该邮箱未注册");
            return response;
        }

        User user = userOpt.get();
        String code = verificationCodeService.generateCode();
        verificationCodeService.saveCode(email, code);

        // 发送邮件
        String content = "您正在重置密码，验证码是：" + code + "，5分钟内有效。\n如果不是您本人操作，请忽略此邮件。";
        mailService.sendBookingNotification(email, user.getUsername(), "重置密码验证码", content);

        response.put("success", true);
        response.put("message", "验证码已发送到您的邮箱");
        return response;
    }

    // 第二步：重置密码
    @PostMapping("/reset")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");

        if (email == null || code == null || newPassword == null) {
            response.put("success", false);
            response.put("message", "请填写完整信息");
            return response;
        }

        if (newPassword.length() < 6) {
            response.put("success", false);
            response.put("message", "新密码长度至少6位");
            return response;
        }

        // 验证验证码
        if (!verificationCodeService.verifyCode(email, code)) {
            response.put("success", false);
            response.put("message", "验证码错误或已过期");
            return response;
        }

        // 更新密码
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return response;
        }

        User user = userOpt.get();
        user.setPassword(PasswordUtil.encode(newPassword));
        userRepository.save(user);

        // 清除验证码
        verificationCodeService.clearCode(email);

        response.put("success", true);
        response.put("message", "密码重置成功，请使用新密码登录");
        return response;
    }
}