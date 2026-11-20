package com.example.bookshop.mapper;

import com.example.bookshop.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(Order order);

    List<Order> findByUserId(@Param("userId") Long userId);

    int deleteByUserId(@Param("userId") Long userId);
}
