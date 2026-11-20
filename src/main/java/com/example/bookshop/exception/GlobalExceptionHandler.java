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
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes ra) {
        ra.addFlashAttribute("error","文件过大，请上传不超过 5 MB 的 .xlsx 文件");
        return "redirect:/admin/books/import";
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public String handleStatus(org.springframework.web.server.ResponseStatusException e, Model model,
                               jakarta.servlet.http.HttpServletResponse response) {
        response.setStatus(e.getStatusCode().value());
        model.addAttribute("errorMsg", e.getReason() == null ? "无法访问此页面" : e.getReason());
        return "error";
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("errorMsg", "页面不存在或已停用");
        return "error";
    }

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
