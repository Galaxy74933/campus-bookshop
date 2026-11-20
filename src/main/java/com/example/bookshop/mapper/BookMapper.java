package com.example.bookshop.mapper;

import com.example.bookshop.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMapper {

    int insert(Book book);

    int update(Book book);

    int deleteById(@Param("id") Long id);

    Book findById(@Param("id") Long id);

    Book findByIdForUpdate(@Param("id") Long id);

    List<Book> selectList(@Param("keyword") String keyword,
                          @Param("offset") int offset,
                          @Param("size") int size);

    long count(@Param("keyword") String keyword);

    /**
     * 条件扣减库存，库存不足时影响行数为 0（并发安全，防超卖）。
     */
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);
}
