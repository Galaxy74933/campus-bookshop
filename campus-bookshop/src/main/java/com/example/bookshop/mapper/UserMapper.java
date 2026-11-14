package com.example.bookshop.mapper;

import com.example.bookshop.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserMapper {

    int insert(User user);

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    /**
     * 条件扣减购书款，余额不足时影响行数为 0。
     */
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    int deleteById(@Param("id") Long id);
}
