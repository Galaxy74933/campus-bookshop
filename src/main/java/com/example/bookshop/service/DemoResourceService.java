package com.example.bookshop.service;

import com.example.bookshop.dto.BookForm;
import com.example.bookshop.mapper.CatalogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Import the classroom examples once, preserving all later user edits/deletions. */
@Service
public class DemoResourceService {
    private final JdbcTemplate jdbc;
    private final CatalogMapper catalog;
    private final CategoryService categories;
    private final BookService books;
    private final ObjectMapper json;

    public DemoResourceService(JdbcTemplate jdbc, CatalogMapper catalog, CategoryService categories,
                               BookService books, ObjectMapper json) {
        this.jdbc=jdbc; this.catalog=catalog; this.categories=categories; this.books=books; this.json=json;
    }

    @Transactional(rollbackFor=Exception.class)
    public void initialize() throws IOException {
        if (jdbc.update("INSERT IGNORE INTO t_app_migration(name) VALUES('demo-resources-v1')")==0) return;
        try (var input=new ClassPathResource("db/demo-resources.json").getInputStream()) {
            for (var row:json.readTree(input)) {
                String title=row.get("title").asText();
                if (jdbc.queryForObject("SELECT COUNT(*) FROM t_book WHERE title=?",Integer.class,title)>0) continue;
                String categoryName=row.get("category").asText();
                Long categoryId=catalog.categoryId(categoryName);
                if (categoryId==null) { categories.save(null,categoryName); categoryId=catalog.categoryId(categoryName); }
                BookForm f=new BookForm();
                f.setTitle(title); f.setCategoryId(categoryId);
                f.setResourceType(row.get("resourceType").asText()); f.setLanguage(row.get("language").asText());
                f.setPrice(new BigDecimal(row.get("price").asText())); f.setStock(row.get("stock").asInt());
                f.setAuthor(row.get("author").asText()); f.setAuthorUnit(row.get("authorUnit").asText());
                f.setPublisher(row.get("publisher").asText()); f.setPublishDate(LocalDate.parse(row.get("publishDate").asText()));
                f.setDescription(row.get("description").asText());
                books.create(f);
            }
        }
    }
}
