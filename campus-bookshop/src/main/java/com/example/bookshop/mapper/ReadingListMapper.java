package com.example.bookshop.mapper;
import com.example.bookshop.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ReadingListMapper {
    @Select("SELECT r.*, (SELECT COUNT(*) FROM t_reading_list_book i WHERE i.list_id=r.id) AS book_count FROM t_reading_list r WHERE r.public_list=TRUE OR r.owner_id=#{ownerId} ORDER BY r.id DESC")
    List<ReadingList> visible(Long ownerId);
    @Select("SELECT * FROM t_reading_list WHERE id=#{id}")
    ReadingList find(Long id);
    @Select("SELECT * FROM t_reading_list WHERE id=#{id} FOR UPDATE")
    ReadingList lock(Long id);
    @Select("SELECT id FROM t_reading_list WHERE owner_id=#{ownerId} AND name=#{name}")
    Long named(@Param("ownerId") Long ownerId,@Param("name") String name);
    @Insert("INSERT INTO t_reading_list(owner_id,name,description,public_list) VALUES(#{ownerId},#{name},#{description},#{publicList})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(ReadingList list);
    @Update("UPDATE t_reading_list SET name=#{name}, description=#{description} WHERE id=#{id}")
    int update(ReadingList list);
    @Delete("DELETE FROM t_reading_list WHERE id=#{id}")
    int delete(Long id);
    List<Book> books(Long id);
    @Select("SELECT COUNT(*) FROM t_reading_list_book WHERE list_id=#{listId} AND book_id=#{bookId}")
    int contains(@Param("listId") Long listId,@Param("bookId") Long bookId);
    @Insert("INSERT INTO t_reading_list_book(list_id,book_id) VALUES(#{listId},#{bookId})")
    int add(@Param("listId") Long listId,@Param("bookId") Long bookId);
    @Delete("DELETE FROM t_reading_list_book WHERE list_id=#{listId} AND book_id=#{bookId}")
    int remove(@Param("listId") Long listId,@Param("bookId") Long bookId);
}

