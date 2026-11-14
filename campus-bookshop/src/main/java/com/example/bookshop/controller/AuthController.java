package com.example.bookshop.controller;

import com.example.bookshop.dto.LoginForm;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.dto.RegisterForm;
import com.example.bookshop.entity.User;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Tag(name = "认证管理", description = "用户登录、注册、登出等认证相关接口")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/reader/entry")
    public String readerEntry() {
        return "reader/entry";
    }

    @GetMapping("/admin/entry")
    public String adminEntry() {
        return "admin/entry";
    }

    @Operation(summary = "显示用户登录页面", description = "返回用户登录页面")
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @Operation(summary = "用户登录", description = "普通用户登录系统，登录成功后跳转到书城首页")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录失败，返回登录页并显示错误提示"),
            @ApiResponse(responseCode = "302", description = "登录成功，重定向到书城")
    })
    @PostMapping("/login")
    public String login(@Parameter(description = "登录表单信息") LoginForm form, HttpSession session, Model model) {
        try {
            User user = userService.login(form.getUsername(), form.getPassword());
            if (!"READER".equals(user.getRole())) {
                model.addAttribute("error", "该账号为管理员账号，请从管理员入口登录");
                model.addAttribute("username", form.getUsername());
                return "login";
            }
            session.setAttribute(AuthInterceptor.SESSION_KEY,
                    new LoginUser(user.getId(), user.getUsername(), user.getRole()));
            return "redirect:/shop/books";
        } catch (BizException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", form.getUsername());
            return "login";
        }
    }

    @Operation(summary = "显示读者注册页面", description = "返回独立的读者注册页面")
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @Operation(summary = "读者注册", description = "创建读者账号，注册成功后赠送1000元购书款")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册失败，返回注册页并显示错误提示"),
            @ApiResponse(responseCode = "302", description = "注册成功，重定向到登录页")
    })
    @PostMapping("/register")
    public String register(@Parameter(description = "注册表单信息") RegisterForm form, Model model, RedirectAttributes ra) {
        form.setRole("READER");
        try {
            userService.register(form);
            ra.addFlashAttribute("msg", "注册成功！系统已赠送 1000 元购书款，请登录");
            return "redirect:/login";
        } catch (BizException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", form.getUsername());
            return "register";
        }
    }

    @GetMapping("/admin/register")
    public String adminRegisterPage() {
        return "admin/register";
    }

    @Operation(summary = "管理员注册", description = "创建管理员账号，初始余额为0")
    @PostMapping("/admin/register")
    public String adminRegister(RegisterForm form, Model model, RedirectAttributes ra) {
        form.setRole("ADMIN");
        try {
            userService.register(form);
            ra.addFlashAttribute("msg", "管理员注册成功，请登录");
            return "redirect:/admin/login";
        } catch (BizException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", form.getUsername());
            return "admin/register";
        }
    }

    @Operation(summary = "显示管理员登录页面", description = "返回管理员登录页面")
    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin/login";
    }

    @Operation(summary = "管理员登录", description = "管理员登录后台管理系统")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录失败，返回管理员登录页并显示错误提示"),
            @ApiResponse(responseCode = "302", description = "登录成功，重定向到后台图书列表")
    })
    @PostMapping("/admin/login")
    public String adminLogin(@Parameter(description = "管理员登录表单信息") LoginForm form, HttpSession session, Model model) {
        try {
            User user = userService.login(form.getUsername(), form.getPassword());
            if (!"ADMIN".equals(user.getRole())) {
                model.addAttribute("error", "该账号不是管理员，无法登录后台");
                model.addAttribute("username", form.getUsername());
                return "admin/login";
            }
            session.setAttribute(AuthInterceptor.SESSION_KEY,
                    new LoginUser(user.getId(), user.getUsername(), user.getRole()));
            return "redirect:/admin/books";
        } catch (BizException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", form.getUsername());
            return "admin/login";
        }
    }

    @Operation(summary = "用户登出", description = "用户退出登录，清空会话信息")
    @ApiResponse(responseCode = "302", description = "登出成功，返回读者入口")
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/reader/entry";
    }

    @Operation(summary = "管理员登出", description = "管理员退出登录，清空会话信息")
    @ApiResponse(responseCode = "302", description = "登出成功，返回管理员入口")
    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/entry";
    }
}
