package com.example.bookshop.controller;

import com.example.bookshop.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shop")
@Tag(name = "书城管理", description = "图书浏览、搜索等前台书城相关接口")
public class ShopBookController {

    private final BookService bookService;

    public ShopBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "图书列表", description = "分页显示图书列表，支持关键字搜索")
    @ApiResponse(responseCode = "200", description = "成功返回图书列表页面")
    @GetMapping("/books")
    public String books(@Parameter(description = "搜索关键字") String keyword, 
                       @Parameter(description = "页码，从1开始") Integer page, 
                       Model model) {
        model.addAttribute("pageResult", bookService.page(keyword, page == null ? 1 : page, 8));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "shop/books";
    }
}
