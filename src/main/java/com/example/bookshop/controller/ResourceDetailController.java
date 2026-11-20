package com.example.bookshop.controller;
import com.example.bookshop.service.BookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class ResourceDetailController {
    private final BookService books;
    public ResourceDetailController(BookService books){this.books=books;}
    @GetMapping({"/admin/books/{id}","/shop/books/{id}"})
    public String detail(@PathVariable Long id,HttpServletRequest request,Model model){
        boolean admin=request.getRequestURI().startsWith("/admin/");
        model.addAttribute("fromAi",!admin && "ai".equals(request.getParameter("source")));
        model.addAttribute("book",books.findById(id)); model.addAttribute("admin",admin);
        model.addAttribute("base",admin?"/admin/books":"/shop/books");
        return "catalog/detail";
    }
}
