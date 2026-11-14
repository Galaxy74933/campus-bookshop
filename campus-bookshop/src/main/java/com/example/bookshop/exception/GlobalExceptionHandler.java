package com.example.bookshop.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * 全局兜底异常处理：业务异常带提示回跳来源页，其他异常进错误页。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public String handleBiz(BizException e, RedirectAttributes ra, HttpServletRequest request) {
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:" + backPath(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e, Model model) {
        model.addAttribute("errorMsg", "系统出现异常：" + e.getMessage());
        return "error";
    }

    private String backPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = URI.create(referer);
                String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
                }
            } catch (Exception ignored) {
                // referer 非法时退回首页
            }
        }
        return "/";
    }
}
