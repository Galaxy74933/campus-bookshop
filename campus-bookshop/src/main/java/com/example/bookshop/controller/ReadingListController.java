package com.example.bookshop.controller;
import com.example.bookshop.dto.*;
import com.example.bookshop.entity.ReadingList;
import com.example.bookshop.interceptor.AuthInterceptor;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping({"/admin/reading-lists","/shop/reading-lists"})
public class ReadingListController {
    private final ReadingListService lists;
    private final BookService books;
    public ReadingListController(ReadingListService lists,BookService books) { this.lists=lists; this.books=books; }
    private LoginUser user(HttpSession s) { return (LoginUser)s.getAttribute(AuthInterceptor.SESSION_KEY); }
    private String base(LoginUser u) { return "ADMIN".equals(u.getRole()) ? "/admin/reading-lists" : "/shop/reading-lists"; }
    private void common(Model m,LoginUser u) { m.addAttribute("admin","ADMIN".equals(u.getRole())); m.addAttribute("base",base(u)); }
    @GetMapping public String index(HttpSession s,Model m) {
        LoginUser u=user(s); common(m,u);
        var all=lists.visible(u);
        m.addAttribute("publicLists",all.stream().filter(ReadingList::isPublicList).toList());
        m.addAttribute("personalLists",all.stream().filter(r -> !r.isPublicList()).toList());
        return "reading/index";
    }
    @GetMapping("/add") public String add(HttpSession s,Model m) {
        common(m,user(s)); m.addAttribute("readingList",new ReadingList()); return "reading/form";
    }
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,HttpSession s,Model m) {
        LoginUser u=user(s); common(m,u); m.addAttribute("readingList",lists.require(id,u,true)); return "reading/form";
    }
    @GetMapping("/{id}") public String detail(@PathVariable Long id,@ModelAttribute("q") BookQuery q,HttpSession s,Model m) {
        LoginUser u=user(s); common(m,u); ReadingList r=lists.require(id,u,false);
        boolean editable=lists.editable(r,u);
        m.addAttribute("readingList",r); m.addAttribute("editable",editable); m.addAttribute("books",lists.books(id,u));
        if(editable) m.addAttribute("pageResult",books.search(q,q.getSize(),false));
        return "reading/detail";
    }
    @PostMapping({ "", "/{id}" })
    public String save(@PathVariable(required=false) Long id,@RequestParam String name,@RequestParam(defaultValue="") String description,
                       HttpSession s,Model m,RedirectAttributes ra) {
        LoginUser u=user(s); common(m,u);
        try { Long saved=lists.save(id,name,description,u); ra.addFlashAttribute("msg","书单已保存"); return "redirect:"+base(u)+"/"+saved; }
        catch(BizException e) {
            ReadingList r=new ReadingList(); r.setId(id); r.setName(name); r.setDescription(description);
            m.addAttribute("readingList",r); m.addAttribute("error",e.getMessage()); return "reading/form";
        }
    }
    @PostMapping("/{id}/delete") public String delete(@PathVariable Long id,HttpSession s,RedirectAttributes ra) {
        LoginUser u=user(s); lists.delete(id,u); ra.addFlashAttribute("msg","书单已删除，图书保留"); return "redirect:"+base(u);
    }
    @PostMapping("/{id}/books") public String addBook(@PathVariable Long id,@RequestParam Long bookId,@RequestParam(defaultValue="") String source,HttpSession s,RedirectAttributes ra) {
        LoginUser u=user(s);
        try { lists.add(id,bookId,u); ra.addFlashAttribute("msg","已加入书单"); }
        catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); }
        return "ai".equals(source) && "READER".equals(u.getRole()) ? "redirect:/shop/ai" : "redirect:"+base(u)+"/"+id;
    }
    @PostMapping("/{id}/books/{bookId}/remove") public String remove(@PathVariable Long id,@PathVariable Long bookId,HttpSession s,RedirectAttributes ra) {
        LoginUser u=user(s); lists.remove(id,bookId,u); ra.addFlashAttribute("msg","已从书单移除"); return "redirect:"+base(u)+"/"+id;
    }
    @GetMapping("/choose") public String choose(@RequestParam Long bookId,@RequestParam(defaultValue="") String source,HttpSession s,Model m) {
        LoginUser u=user(s); common(m,u); m.addAttribute("book",books.findById(bookId));
        m.addAttribute("fromAi","ai".equals(source) && "READER".equals(u.getRole()));
        m.addAttribute("lists",lists.visible(u).stream().filter(r -> lists.editable(r,u)).toList()); return "reading/choose";
    }
}
