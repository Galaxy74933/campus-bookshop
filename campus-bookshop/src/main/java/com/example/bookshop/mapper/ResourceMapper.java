package com.example.bookshop.mapper;
import com.example.bookshop.entity.Book;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ResourceMapper {
    @Delete("DELETE FROM t_resource_info WHERE book_id=#{id}") int clearInfo(Long id);
    @Insert("INSERT INTO t_resource_info(book_id,resource_type,language,author_unit,publisher,publish_date,identifier,description) VALUES(#{id},#{resourceType},#{language},#{authorUnit},#{publisher},#{publishDate},#{identifier},#{description})")
    int insertInfo(Book book);
    @Delete("DELETE FROM t_resource_category WHERE book_id=#{id}") int clearCategories(Long id);
    @Insert("INSERT INTO t_resource_category(book_id,category_id) VALUES(#{id},#{categoryId})")
    int addCategory(@Param("id") Long id,@Param("categoryId") Long categoryId);
    @Select("SELECT category_id FROM t_resource_category WHERE book_id=#{id} ORDER BY category_id")
    List<Long> categories(Long id);
}
