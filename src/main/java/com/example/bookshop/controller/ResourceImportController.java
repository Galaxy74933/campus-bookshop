package com.example.bookshop.controller;

import com.example.bookshop.exception.BizException;
import com.example.bookshop.service.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import com.example.bookshop.dto.LoginUser;
import com.example.bookshop.interceptor.AuthInterceptor;

@Controller
@RequestMapping("/admin/books/import")
public class ResourceImportController {
    private final ResourceImportService importer;
    private final CategoryService categories;
    private final ImportBatchService batches;
    public ResourceImportController(ResourceImportService importer,CategoryService categories,ImportBatchService batches) { this.importer=importer; this.categories=categories; this.batches=batches; }
    @GetMapping public String page(Model model) {
        model.addAttribute("categories",categories.list()); return "admin/book-import";
    }
    @PostMapping public String upload(@RequestParam("file") MultipartFile file,Model model,RedirectAttributes ra,
            @SessionAttribute(AuthInterceptor.SESSION_KEY) LoginUser user) {
        try {
            int count=importer.importFile(file,user.getId());
            ra.addFlashAttribute("msg","成功导入 "+count+" 条资源"); return "redirect:/admin/books";
        } catch(ResourceImportService.ImportException e) {
            model.addAttribute("error",e.getMessage()); model.addAttribute("importErrors",e.getErrors());
        } catch(BizException e) { model.addAttribute("error",e.getMessage()); }
        catch(org.springframework.dao.DataAccessException e) { model.addAttribute("error","导入失败，未新增任何资源。请检查数据库连接及分类是否发生变化后重试。"); }
        return page(model);
    }
    @PostMapping("/{id}/undo")
    public String undo(@PathVariable Long id,@SessionAttribute(AuthInterceptor.SESSION_KEY) LoginUser user,RedirectAttributes ra) {
        try { ra.addFlashAttribute("msg","已撤回最近一次导入，删除 "+batches.undo(id,user.getId())+" 条资源；已删除的自动跳过，历史订单保留"); }
        catch(BizException e) { ra.addFlashAttribute("error",e.getMessage()); }
        catch(org.springframework.dao.DataAccessException e) { ra.addFlashAttribute("error","撤回失败，本次操作未删除任何资源，请刷新后重试"); }
        return "redirect:/admin/books";
    }
    @GetMapping("/{kind:template|demo}")
    public ResponseEntity<ClassPathResource> download(@PathVariable String kind) {
        String name="demo".equals(kind)?"资源导入演示.xlsx":"资源导入模板.xlsx";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString())
            .body(new ClassPathResource("import/"+kind+".xlsx"));
    }
}
