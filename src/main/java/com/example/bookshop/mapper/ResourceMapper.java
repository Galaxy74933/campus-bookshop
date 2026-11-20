package com.example.bookshop.mapper;
import com.example.bookshop.entity.Book;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ResourceMapper {
    @Select("SELECT id FROM t_category ORDER BY id FOR UPDATE")
    List<Long> lockImportCategories();
    @Select("SELECT COUNT(*) FROM t_book b LEFT JOIN t_resource_info r ON r.book_id=b.id WHERE (#{identifier} != '' AND LOWER(TRIM(r.identifier))=LOWER(#{identifier})) OR (LOWER(TRIM(b.title))=LOWER(#{title}) AND LOWER(TRIM(COALESCE(b.author,'')))=LOWER(#{author}) AND COALESCE(r.resource_type,'图书')=#{resourceType})")
    int importDuplicates(Book book);
    @Delete("DELETE FROM t_resource_info WHERE book_id=#{id}") int clearInfo(Long id);
    @Insert("INSERT INTO t_resource_info(book_id,resource_type,language,author_unit,publisher,publish_date,identifier,description) VALUES(#{id},#{resourceType},#{language},#{authorUnit},#{publisher},#{publishDate},#{identifier},#{description})")
    int insertInfo(Book book);
    @Delete("DELETE FROM t_resource_category WHERE book_id=#{id}") int clearCategories(Long id);
    @Insert("INSERT INTO t_resource_category(book_id,category_id) VALUES(#{id},#{categoryId})")
    int addCategory(@Param("id") Long id,@Param("categoryId") Long categoryId);
    @Select("SELECT category_id FROM t_resource_category WHERE book_id=#{id} ORDER BY category_id")
    List<Long> categories(Long id);
}
