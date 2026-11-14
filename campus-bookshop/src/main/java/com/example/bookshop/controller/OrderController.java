package com.example.bookshop.controller;

import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Tag(name = "订单管理", description = "订单查看和历史订单查询等接口")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订单列表", description = "查看用户的历史订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回订单列表页面"),
            @ApiResponse(responseCode = "302", description = "未登录或非读者角色，重定向到读者登录页")
    })
    @GetMapping("/shop/orders")
    public String orders(HttpSession session, Model model) {
        LoginUser user = (LoginUser) session.getAttribute(AuthInterceptor.SESSION_KEY);
        model.addAttribute("orders", orderService.listByUser(user.getId()));
        return "shop/orders";
    }
}
