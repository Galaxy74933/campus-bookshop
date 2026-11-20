package com.example.bookshop.dto;

/**
 * 存放在 Session 中的登录用户信息（不含余额，余额每次从数据库读取最新值）。
 */
public class LoginUser {

    private final Long id;
    private final String username;
    private final String role;

    public LoginUser(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
