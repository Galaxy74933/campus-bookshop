package com.example.bookshop.controller;
import com.example.bookshop.dto.BookQuery;
import com.example.bookshop.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/shop")
public class ShopBookController {
    private final BookService books;
    private final CategoryService categories;
    public ShopBookController(BookService books,CategoryService categories) { this.books=books; this.categories=categories; }
    @GetMapping("/books") public String list(@ModelAttribute("q") BookQuery q,Model model) {
        model.addAttribute("pageResult",books.search(q,8,false)); model.addAttribute("categories",categories.list());
        model.addAttribute("admin",false); model.addAttribute("base","/shop/books");
        return "catalog/books";
    }
}
