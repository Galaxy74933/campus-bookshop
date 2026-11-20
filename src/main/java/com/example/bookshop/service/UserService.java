package com.example.bookshop.service;

import com.example.bookshop.dto.RegisterForm;
import com.example.bookshop.entity.User;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class UserService {

    /** 注册赠送的购书款金额 */
    public static final BigDecimal GIFT_AMOUNT = new BigDecimal("1000.00");

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户注册：读者注册成功即赠送 1000 元购书款，管理员余额为 0。
     */
    @Transactional
    public void register(RegisterForm form) {
        String username = form.getUsername() == null ? "" : form.getUsername().trim();
        String password = form.getPassword() == null ? "" : form.getPassword();
        String confirm = form.getConfirmPassword() == null ? "" : form.getConfirmPassword();
        String role = form.getRole() == null ? "" : form.getRole().trim().toUpperCase(Locale.ROOT);

        if (username.isEmpty()) {
            throw new BizException("用户名不能为空");
        }
        if (username.length() > 50) {
            throw new BizException("用户名不能超过 50 个字符");
        }
        if (password.isEmpty()) {
            throw new BizException("密码不能为空");
        }
        if (password.length() > 50) {
            throw new BizException("密码不能超过 50 个字符");
        }
        if (!password.equals(confirm)) {
            throw new BizException("两次输入的密码不一致");
        }
        if (!"READER".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException("请选择有效的注册角色");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new BizException("用户名已存在，请换一个");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setBalance("READER".equals(role) ? GIFT_AMOUNT : BigDecimal.ZERO);
        userMapper.insert(user);
    }

    /**
     * 登录校验（明文比对）。
     */
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username == null ? "" : username.trim());
        if (user == null || !user.getPassword().equals(password == null ? "" : password)) {
            throw new BizException("用户名或密码错误");
        }
        return user;
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }
}
