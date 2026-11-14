package com.example.bookshop.controller;

import com.example.bookshop.dto.BookForm;
import com.example.bookshop.entity.Book;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@Tag(name = "图书管理", description = "图书的增删改查等后台管理接口")
public class AdminBookController {

    private final BookService bookService;

    public AdminBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "管理首页", description = "重定向到图书管理列表页面")
    @GetMapping
    public String adminHome() {
        return "redirect:/admin/books";
    }

    @Operation(summary = "图书列表", description = "分页显示图书列表，支持关键字搜索")
    @ApiResponse(responseCode = "200", description = "成功返回图书管理页面")
    @GetMapping("/books")
    public String list(@Parameter(description = "搜索关键字") String keyword, 
                     @Parameter(description = "页码，从1开始") Integer page, 
                     Model model) {
        model.addAttribute("pageResult", bookService.page(keyword, page == null ? 1 : page, 10));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "admin/books";
    }

    @Operation(summary = "添加图书页面", description = "返回图书添加表单页面")
    @GetMapping("/books/add")
    public String addPage(Model model) {
        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("isEdit", false);
        return "admin/book-form";
    }

    @Operation(summary = "创建图书", description = "创建新的图书信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "校验失败，返回表单页并显示错误提示"),
            @ApiResponse(responseCode = "302", description = "创建成功，重定向到图书列表")
    })
    @PostMapping("/books")
    public String create(@Parameter(description = "图书表单信息") @Valid BookForm bookForm, 
                        BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("error", firstError(result));
            return "admin/book-form";
        }
        bookService.create(bookForm);
        ra.addFlashAttribute("msg", "图书创建成功");
        return "redirect:/admin/books";
    }

    @Operation(summary = "编辑图书页面", description = "返回图书编辑表单页面")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回编辑页面"),
            @ApiResponse(responseCode = "302", description = "图书不存在，携带错误提示重定向")
    })
    @GetMapping("/books/{id}/edit")
    public String editPage(@Parameter(description = "图书ID") @PathVariable Long id, Model model) {
        Book book = bookService.findById(id);
        BookForm form = new BookForm();
        form.setId(book.getId());
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setPrice(book.getPrice());
        form.setStock(book.getStock());
        model.addAttribute("bookForm", form);
        model.addAttribute("isEdit", true);
        return "admin/book-form";
    }

    @Operation(summary = "更新图书", description = "更新指定图书的信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "校验失败，返回表单页并显示错误提示"),
            @ApiResponse(responseCode = "302", description = "更新成功，或图书不存在时携带错误提示重定向")
    })
    @PostMapping("/books/{id}")
    public String update(@Parameter(description = "图书ID") @PathVariable Long id, 
                       @Parameter(description = "图书表单信息") @Valid BookForm bookForm, 
                       BindingResult result,
                       Model model, RedirectAttributes ra) {
        bookForm.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("error", firstError(result));
            return "admin/book-form";
        }
        bookService.update(id, bookForm);
        ra.addFlashAttribute("msg", "图书更新成功");
        return "redirect:/admin/books";
    }

    @Operation(summary = "删除图书", description = "删除指定的图书信息")
    @ApiResponse(responseCode = "302", description = "删除成功或失败均重定向回图书列表（失败带错误提示）")
    @PostMapping("/books/{id}/delete")
    public String delete(@Parameter(description = "图书ID") @PathVariable Long id, RedirectAttributes ra) {
        try {
            bookService.delete(id);
            ra.addFlashAttribute("msg", "图书已删除");
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    private String firstError(BindingResult result) {
        if (result.hasFieldErrors("price")) {
            return "价格必须是有效数字，且大于 0，最多两位小数";
        }
        if (result.hasFieldErrors("stock")) {
            return "库存必须是有效整数，且不小于 0";
        }
        if (result.getFieldError() != null) {
            return result.getFieldError().getDefaultMessage();
        }
        return "输入有误";
    }
}
