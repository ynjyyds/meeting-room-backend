package com.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户注册
     * POST /api/user/register
     * 请求体：{"username": "xxx", "password": "xxx", "email": "xxx"}
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        Map<String, Object> response = new HashMap<>();

        // 检查用户名是否已存在
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            response.put("success", false);
            response.put("message", "用户名已存在");
            return response;
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setEmail(email);
        user.setRole("USER");  // 默认角色
        user.setCreateTime(LocalDateTime.now());

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "注册成功");
        return response;
    }

    /**
     * 用户登录
     * POST /api/user/login
     * 请求体：{"username": "xxx", "password": "xxx"}
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Map<String, Object> response = new HashMap<>();

        // 查找用户
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return response;
        }

        User user = optionalUser.get();

        // 验证密码
        if (!PasswordUtil.matches(password, user.getPassword())) {
            response.put("success", false);
            response.put("message", "密码错误");
            return response;
        }

        // 生成 JWT token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        response.put("success", true);
        response.put("message", "登录成功");
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        return response;
    }
}