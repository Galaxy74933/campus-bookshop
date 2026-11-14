package com.example.bookshop.mapper;

import com.example.bookshop.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int insertBatch(@Param("items") List<OrderItem> items);

    List<OrderItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);

    int deleteByOrderIds(@Param("orderIds") List<Long> orderIds);
}
