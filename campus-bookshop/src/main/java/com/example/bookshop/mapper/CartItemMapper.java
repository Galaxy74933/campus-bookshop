package com.example.bookshop.mapper;

import com.example.bookshop.dto.CartItemView;
import com.example.bookshop.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemMapper {

    /**
     * 加入购物车：同一用户同一图书存在记录时数量累加。
     */
    int upsert(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("quantity") int quantity);

    CartItem findByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    CartItem findById(@Param("id") Long id);

    int updateQuantity(@Param("id") Long id, @Param("quantity") int quantity);

    int deleteById(@Param("id") Long id);

    int deleteByUserId(@Param("userId") Long userId);

    List<CartItem> findByUserId(@Param("userId") Long userId);

    List<CartItemView> selectViewByUserId(@Param("userId") Long userId);
}
