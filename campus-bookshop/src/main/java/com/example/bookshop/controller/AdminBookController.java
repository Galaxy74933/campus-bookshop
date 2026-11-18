package com.example.bookshop.controller;
import com.example.bookshop.dto.*;
import com.example.bookshop.entity.Book;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin")
public class AdminBookController {
    private final BookService books;
    private final CategoryService categories;
    public AdminBookController(BookService books,CategoryService categories) { this.books=books; this.categories=categories; }
    @GetMapping public String home() { return "redirect:/admin/books"; }
    @GetMapping("/books")
    public String list(@ModelAttribute("q") BookQuery q,Model model) {
        model.addAttribute("pageResult",books.search(q,10,true));
        model.addAttribute("categories",categories.list());
        model.addAttribute("threshold",books.threshold());
        model.addAttribute("admin",true); model.addAttribute("base","/admin/books");
        return "catalog/books";
    }
    private String form(Model m,boolean edit) {
        m.addAttribute("isEdit",edit); m.addAttribute("categories",categories.list()); return "admin/book-form";
    }
    @GetMapping("/books/add") public String add(Model m) { m.addAttribute("bookForm",new BookForm()); return form(m,false); }
    @GetMapping("/books/{id}/edit") public String edit(@PathVariable Long id,Model m) {
        Book b=books.findById(id); BookForm f=new BookForm();
        f.setId(id); f.setTitle(b.getTitle()); f.setAuthor(b.getAuthor()); f.setPrice(b.getPrice()); f.setStock(b.getStock()); f.setCategoryId(b.getCategoryId());
        f.setCategoryIds(b.getCategoryIds()); f.setResourceType(b.getResourceType()); f.setLanguage(b.getLanguage());
        f.setPublisher(b.getPublisher()); f.setPublishDate(b.getPublishDate()); f.setIdentifier(b.getIdentifier());
        f.setDescription(b.getDescription()); f.setAuthorUnit(b.getAuthorUnit());
        m.addAttribute("bookForm",f); return form(m,true);
    }
    @PostMapping({"/books","/books/{id}"})
    public String save(@PathVariable(required=false) Long id,@Valid @ModelAttribute("bookForm") BookForm f,
                       BindingResult result,Model m,RedirectAttributes ra) {
        f.setId(id);
        if (result.hasErrors()) { m.addAttribute("error","请检查下方字段提示，带 * 的项目为必填"); return form(m,id!=null); }
        try { if(id==null) books.create(f); else books.update(id,f); }
        catch(BizException e) { m.addAttribute("error",e.getMessage()); return form(m,id!=null); }
        ra.addFlashAttribute("msg","资源已保存"); return "redirect:/admin/books";
    }
    @PostMapping("/books/{id}/delete") public String delete(@PathVariable Long id,RedirectAttributes ra) {
        try { books.delete(id); ra.addFlashAttribute("msg","图书已删除，历史订单保留"); }
        catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/books";
    }
}
