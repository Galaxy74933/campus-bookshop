package com.example.bookshop.controller;
import com.example.bookshop.service.CategoryService;
import com.example.bookshop.exception.BizException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/categories")
public class CategoryController {
    private final CategoryService categories;
    public CategoryController(CategoryService categories) { this.categories=categories; }
    @GetMapping public String list(Model m) { m.addAttribute("categories",categories.list()); return "admin/categories"; }
    @PostMapping public String save(@RequestParam(required=false) Long id,@RequestParam String name,RedirectAttributes ra) {
        try { categories.save(id,name); ra.addFlashAttribute("msg","分类已保存"); }
        catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/categories";
    }
    @PostMapping("/{id}/delete") public String delete(@PathVariable Long id,RedirectAttributes ra) {
        try { categories.delete(id); ra.addFlashAttribute("msg","分类已删除"); }
        catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/categories";
    }
}
