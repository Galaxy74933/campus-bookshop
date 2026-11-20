package com.example.bookshop.mapper;
import com.example.bookshop.entity.*;
import com.example.bookshop.dto.BookQuery;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface CatalogMapper {
    @Select("SELECT c.*, COUNT(bc.book_id) AS book_count FROM t_category c LEFT JOIN (SELECT book_id,category_id FROM t_book_category UNION SELECT book_id,category_id FROM t_resource_category) bc ON bc.category_id=c.id GROUP BY c.id,c.name ORDER BY c.id")
    List<Category> categories();
    @Select("SELECT * FROM t_category WHERE id=#{id}")
    Category category(Long id);
    @Select("SELECT id FROM t_category WHERE name=#{name}")
    Long categoryId(String name);
    @Insert("INSERT INTO t_category(name) VALUES(#{name})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertCategory(Category category);
    @Update("UPDATE t_category SET name=#{name} WHERE id=#{id}")
    int updateCategory(Category category);
    @Delete("DELETE FROM t_category WHERE id=#{id}")
    int deleteCategory(Long id);
    @Delete("DELETE FROM t_book_category WHERE book_id=#{bookId}")
    int clearCategory(Long bookId);
    @Insert("INSERT INTO t_book_category(book_id,category_id) VALUES(#{bookId},#{categoryId})")
    int assignCategory(@Param("bookId") Long bookId,@Param("categoryId") Long categoryId);
    @Delete("DELETE FROM t_cart_item WHERE book_id=#{bookId}")
    int clearCartBook(Long bookId);
    @Insert("INSERT IGNORE INTO t_app_migration(name) VALUES('catalog-v1')")
    int claimInitialization();
    List<Book> search(@Param("q") BookQuery query,@Param("offset") int offset,@Param("size") int size);
    long count(@Param("q") BookQuery query);
}

