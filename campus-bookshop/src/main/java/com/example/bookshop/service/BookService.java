package com.example.bookshop.service;

import com.example.bookshop.dto.BookForm;
import com.example.bookshop.dto.PageResult;
import com.example.bookshop.entity.Book;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.BookMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookMapper bookMapper;
    private final AcademicService academicService;

    public BookService(BookMapper bookMapper, AcademicService academicService) {
        this.bookMapper = bookMapper;
        this.academicService = academicService;
    }

    /**
     * 分页查询图书，keyword 按书名模糊匹配。
     */
    public PageResult<Book> page(String keyword, int page, int size) {
        String kw = keyword == null ? "" : keyword.trim();
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        long total = bookMapper.count(kw);
        int totalPages = (int) Math.ceil(total / (double) size);
        if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }
        List<Book> records = bookMapper.selectList(kw, (page - 1) * size, size);
        return new PageResult<>(records, total, page, size, totalPages);
    }

    public Book findById(Long id) {
        Book book = bookMapper.findById(id);
        if (book == null) {
            throw new BizException("图书不存在或已下架");
        }
        return book;
    }

    @Transactional
    public void create(BookForm form) {
        List<Long> courseIds = academicService.validateCourseIds(form.getCourseIds());
        Book book = new Book();
        book.setTitle(form.getTitle().trim());
        book.setAuthor(form.getAuthor() == null ? "" : form.getAuthor().trim());
        book.setPrice(form.getPrice());
        book.setStock(form.getStock());
        bookMapper.insert(book);
        academicService.replaceBookCourses(book.getId(), courseIds);
    }

    @Transactional
    public void update(Long id, BookForm form) {
        List<Long> courseIds = academicService.validateCourseIds(form.getCourseIds());
        findById(id);
        Book book = new Book();
        book.setId(id);
        book.setTitle(form.getTitle().trim());
        book.setAuthor(form.getAuthor() == null ? "" : form.getAuthor().trim());
        book.setPrice(form.getPrice());
        book.setStock(form.getStock());
        if (bookMapper.update(book) == 0) throw new BizException("图书不存在，请刷新列表");
        academicService.replaceBookCourses(id, courseIds);
    }

    public void delete(Long id) {
        findById(id);
        bookMapper.deleteById(id);
    }
}
