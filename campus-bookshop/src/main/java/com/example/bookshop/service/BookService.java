package com.example.bookshop.service;
import com.example.bookshop.dto.*;
import com.example.bookshop.entity.Book;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.*;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BookService {
    private final BookMapper books;
    private final ResourceMapper resources;
    private final CatalogMapper catalog;
    private final CategoryService categories;
    private final Validator validator;
    private final int threshold;
    public BookService(ResourceMapper resources,BookMapper books,CatalogMapper catalog,CategoryService categories,Validator validator,
                       @Value("${bookshop.low-stock-threshold:5}") int threshold) {
        this.resources=resources; this.books=books; this.catalog=catalog; this.categories=categories; this.validator=validator;
        this.threshold=Math.max(1,threshold);
    }
    public int threshold() { return threshold; }
    public PageResult<Book> page(String keyword,int page,int size) {
        BookQuery q=new BookQuery(); q.setKeyword(keyword); q.setPage(page); return search(q,size,false);
    }
    public PageResult<Book> search(BookQuery q,int size,boolean admin) {
        q.normalize(threshold,admin);
        size=Math.max(1,Math.min(size,100));
        long total=catalog.count(q);
        int totalPages=(int)Math.ceil(total/(double)size);
        int page=Math.min(q.getPage(),Math.max(1,totalPages));
        q.setPage(page);
        return new PageResult<>(catalog.search(q,(page-1)*size,size),total,page,size,totalPages);
    }
    public Book findById(Long id) {
        Book book=books.findById(id);
        if (book==null) throw new BizException("图书不存在");
        book.setCategoryIds(resources.categories(id));
        return book;
    }
    private Book validated(BookForm f) {
        var violations=validator.validate(f);
        if (!violations.isEmpty()) throw new BizException(violations.iterator().next().getMessage());
        categories.require(f.getCategoryId());
        if (!ResourceOptions.TYPES.contains(f.getResourceType())) throw new BizException("请选择有效资源类型");
        if (f.getLanguage()!=null && !f.getLanguage().isBlank() && !ResourceOptions.LANGUAGES.contains(f.getLanguage())) throw new BizException("请选择有效语言");
        if (f.getCategoryIds()!=null) f.getCategoryIds().forEach(categories::require);
        Book b=new Book(); b.setTitle(f.getTitle().trim()); b.setAuthor(f.getAuthor()==null ? "" : f.getAuthor().trim());
        b.setResourceType(f.getResourceType()); b.setLanguage(trim(f.getLanguage())); b.setAuthorUnit(trim(f.getAuthorUnit()));
        b.setPublisher(trim(f.getPublisher())); b.setPublishDate(f.getPublishDate()); b.setIdentifier(trim(f.getIdentifier())); b.setDescription(trim(f.getDescription()));
        b.setPrice(b.isPurchasable()?f.getPrice():java.math.BigDecimal.ZERO); b.setStock(b.isPurchasable()?f.getStock():0); return b;
    }
    @Transactional public void create(BookForm f) {
        Book b=validated(f); books.insert(b); assign(b.getId(),f.getCategoryId()); saveMetadata(b,f);
    }
    @Transactional public void update(Long id,BookForm f) {
        Book b=validated(f); findById(id); b.setId(id);
        if (books.update(b)==0) throw new BizException("图书不存在");
        assign(id,f.getCategoryId()); saveMetadata(b,f);
        if (!b.isPurchasable()) catalog.clearCartBook(id);
    }
    private static String trim(String s) { return s==null?"":s.trim(); }
    private void saveMetadata(Book b,BookForm f) {
        resources.clearInfo(b.getId()); resources.insertInfo(b);
        resources.clearCategories(b.getId());
        if (f.getCategoryIds()!=null) for(Long id:f.getCategoryIds().stream().distinct().toList()) {
            if (!id.equals(f.getCategoryId())) resources.addCategory(b.getId(),id);
        }
    }
    private void assign(Long bookId,Long categoryId) {
        catalog.clearCategory(bookId);
        try { catalog.assignCategory(bookId,categoryId); }
        catch(DataIntegrityViolationException e) { throw new BizException("分类已变化，请刷新后重试"); }
    }
    @Transactional public void delete(Long id) {
        findById(id); catalog.clearCartBook(id); books.deleteById(id);
    }
}
