package com.example.bookshop.controller;

import com.example.bookshop.dto.CartItemView;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.entity.Order;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.service.CartService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/shop/cart")
@Tag(name = "购物车管理", description = "购物车的增删改查和结算等接口")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    public CartController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @Operation(summary = "购物车页面", description = "显示用户的购物车内容")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回购物车页面"),
            @ApiResponse(responseCode = "302", description = "未登录或非读者角色，重定向到读者登录页")
    })
    @GetMapping
    public String cart(HttpSession session, Model model) {
        LoginUser user = currentUser(session);
        List<CartItemView> items = cartService.cartView(user.getId());
        BigDecimal total = items.stream()
                .map(CartItemView::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "shop/cart";
    }

    @Operation(summary = "加入购物车", description = "将指定图书加入购物车")
    @ApiResponse(responseCode = "302", description = "操作后重定向回图书列表（成功提示「已加入购物车」，失败提示原因，如库存不足）")
    @PostMapping("/add")
    public String add(@Parameter(description = "图书ID") @RequestParam Long bookId, 
                     @Parameter(description = "购买数量，默认为1") @RequestParam(defaultValue = "1") Integer quantity,
                     HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        try {
            cartService.add(user.getId(), bookId, quantity);
            ra.addFlashAttribute("msg", "已加入购物车");
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shop/books";
    }

    @Operation(summary = "更新购物车商品数量", description = "更新购物车中指定商品的数量")
    @ApiResponse(responseCode = "302", description = "操作后重定向回购物车页面（成功提示「数量已更新」，失败提示原因，如超出库存）")
    @PostMapping("/update")
    public String update(@Parameter(description = "购物车项目ID") @RequestParam Long itemId, 
                        @Parameter(description = "新的数量") @RequestParam Integer quantity,
                        HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        try {
            cartService.updateQuantity(user.getId(), itemId, quantity);
            ra.addFlashAttribute("msg", "数量已更新");
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shop/cart";
    }

    @Operation(summary = "移除购物车商品", description = "从购物车中移除指定商品")
    @ApiResponse(responseCode = "302", description = "操作后重定向回购物车页面（成功提示「已从购物车移除」，失败提示原因）")
    @PostMapping("/remove")
    public String remove(@Parameter(description = "购物车项目ID") @RequestParam Long itemId, 
                        HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        try {
            cartService.remove(user.getId(), itemId);
            ra.addFlashAttribute("msg", "已从购物车移除");
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shop/cart";
    }

    @Operation(summary = "清空购物车", description = "清空用户购物车中的所有商品")
    @ApiResponse(responseCode = "302", description = "清空成功，重定向回购物车页面（提示「购物车已清空」）")
    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        cartService.clear(user.getId());
        ra.addFlashAttribute("msg", "购物车已清空");
        return "redirect:/shop/cart";
    }

    @Operation(summary = "结算支付", description = "使用购书款余额进行结算支付（整单事务，失败整体回滚）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "支付成功，重定向到订单列表"),
            @ApiResponse(responseCode = "302", description = "支付失败（购物车为空/库存不足/购书款不足），重定向回购物车页并显示原因")
    })
    @PostMapping("/checkout")
    public String checkout(HttpSession session, RedirectAttributes ra) {
        LoginUser user = currentUser(session);
        try {
            Order order = orderService.checkout(user.getId());
            ra.addFlashAttribute("msg", "支付成功！订单号 " + order.getOrderNo()
                    + "，支付金额 ￥" + order.getTotalAmount().toPlainString());
            return "redirect:/shop/orders";
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/cart";
        }
    }

    private LoginUser currentUser(HttpSession session) {
        return (LoginUser) session.getAttribute(AuthInterceptor.SESSION_KEY);
    }
}
