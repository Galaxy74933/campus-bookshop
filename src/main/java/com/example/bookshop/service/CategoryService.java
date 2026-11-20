package com.example.bookshop.service;
import com.example.bookshop.entity.Category;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.CatalogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
@Service
public class CategoryService {
    private final CatalogMapper mapper;
    public CategoryService(CatalogMapper mapper) { this.mapper=mapper; }
    public List<Category> list() { return mapper.categories(); }
    public Category require(Long id) {
        Category c = id == null ? null : mapper.category(id);
        if (c == null) throw new BizException("请选择有效的图书类别");
        return c;
    }
    @Transactional
    public void save(Long id, String raw) {
        String name=raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length()>100) throw new BizException("分类名称须为1～100个字符");
        if (id != null) require(id);
        Long existing=mapper.categoryId(name);
        if (existing != null && !existing.equals(id)) throw new BizException("分类名称已存在");
        Category c=new Category(); c.setId(id); c.setName(name);
        try {
            if (id == null) mapper.insertCategory(c);
            else if (mapper.updateCategory(c)==0) throw new BizException("分类不存在");
        } catch (DataIntegrityViolationException e) { throw new BizException("分类名称已存在或数据已变化，请刷新后重试"); }
    }
    @Transactional
    public void delete(Long id) {
        require(id);
        try { mapper.deleteCategory(id); }
        catch (DataIntegrityViolationException e) { throw new BizException("该分类已有图书，请先调整图书分类"); }
    }
    @Transactional
    public void initializeDefaults() {
        if (mapper.claimInitialization()==0) return;
        for (String name: List.of("计算机与信息","数学与自然科学","工程技术","经济与管理","人文与社会科学","外语","艺术与设计","综合其他"))
            if (mapper.categoryId(name)==null) save(null,name);
    }
}

