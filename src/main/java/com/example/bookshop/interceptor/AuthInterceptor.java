package com.example.bookshop.interceptor;

import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.entity.User;
import com.example.bookshop.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 登录与角色拦截：/admin/** 仅管理员，/shop/** 仅普通读者。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_KEY = "loginUser";

    private final UserService userService;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        LoginUser user = currentUser(request);
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin")) {
            if (user == null || !"ADMIN".equals(user.getRole())) {
                response.sendRedirect("/admin/login");
                return false;
            }
            return true;
        }
        if (user == null || !"READER".equals(user.getRole())) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null) {
            return;
        }
        // 重定向视图不能注入模型属性：标量属性会被 RedirectView 拼进重定向 URL，
        // 并导致 flash 提示的目标参数匹配失败、提示无法显示
        String viewName = modelAndView.getViewName();
        if (viewName == null || viewName.startsWith("redirect:")) {
            return;
        }
        LoginUser user = currentUser(request);
        if (user == null) {
            return;
        }
        modelAndView.addObject("loginUser", user);
        if (request.getRequestURI().startsWith("/shop")) {
            User dbUser = userService.findById(user.getId());
            if (dbUser != null) {
                modelAndView.addObject("balance", dbUser.getBalance());
            }
        }
    }

    private LoginUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (LoginUser) session.getAttribute(SESSION_KEY);
    }
}
