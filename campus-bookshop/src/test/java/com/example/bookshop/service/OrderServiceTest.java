package com.example.bookshop.service;

import com.example.bookshop.entity.Book;
import com.example.bookshop.entity.Order;
import com.example.bookshop.entity.User;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.BookMapper;
import com.example.bookshop.mapper.CartItemMapper;
import com.example.bookshop.mapper.OrderItemMapper;
import com.example.bookshop.mapper.OrderMapper;
import com.example.bookshop.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 结算支付核心流程测试：正常扣款扣库存、余额不足、库存不足、并发防超卖。
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderService orderService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    BookMapper bookMapper;
    @Autowired
    CartItemMapper cartItemMapper;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;

    private User newUser(BigDecimal balance) {
        User user = new User();
        user.setUsername("test_" + System.nanoTime());
        user.setPassword("123456");
        user.setRole("READER");
        user.setBalance(balance);
        userMapper.insert(user);
        return user;
    }

    private Book newBook(String title, String price, int stock) {
        Book book = new Book();
        book.setTitle(title + "_" + System.nanoTime());
        book.setAuthor("测试作者");
        book.setPrice(new BigDecimal(price));
        book.setStock(stock);
        bookMapper.insert(book);
        return book;
    }

    @Test
    @Transactional
    void checkoutSuccessDeductsBalanceStockAndClearsCart() {
        User user = newUser(new BigDecimal("1000.00"));
        Book b1 = newBook("正常结算书A", "30.00", 10);
        Book b2 = newBook("正常结算书B", "20.50", 5);
        cartItemMapper.upsert(user.getId(), b1.getId(), 2);
        cartItemMapper.upsert(user.getId(), b2.getId(), 1);

        Order order = orderService.checkout(user.getId());

        assertThat(order.getOrderNo()).startsWith("BS");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("80.50");
        assertThat(userMapper.findById(user.getId()).getBalance()).isEqualByComparingTo("919.50");
        assertThat(bookMapper.findById(b1.getId()).getStock()).isEqualTo(8);
        assertThat(bookMapper.findById(b2.getId()).getStock()).isEqualTo(4);
        assertThat(cartItemMapper.findByUserId(user.getId())).isEmpty();
        assertThat(orderItemMapper.selectByOrderIds(List.of(order.getId()))).hasSize(2);
    }

    @Test
    void checkoutFailsWhenBalanceNotEnoughAndRollsBackStock() {
        // 不加 @Transactional：验证 Service 事务真实回滚后的数据状态，结束后清理数据
        User user = newUser(new BigDecimal("50.00"));
        Book book = newBook("余额不足测试书", "60.00", 10);
        cartItemMapper.upsert(user.getId(), book.getId(), 1);

        try {
            assertThatThrownBy(() -> orderService.checkout(user.getId()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("购书款不足");

            // 库存扣减已回滚、购物车保留、未生成订单
            assertThat(bookMapper.findById(book.getId()).getStock()).isEqualTo(10);
            assertThat(cartItemMapper.findByUserId(user.getId())).hasSize(1);
            assertThat(orderMapper.findByUserId(user.getId())).isEmpty();
        } finally {
            cleanup(user);
            bookMapper.deleteById(book.getId());
        }
    }

    @Test
    @Transactional
    void checkoutFailsWhenStockNotEnough() {
        User user = newUser(new BigDecimal("1000.00"));
        Book book = newBook("库存不足测试书", "10.00", 2);
        cartItemMapper.upsert(user.getId(), book.getId(), 3);

        assertThatThrownBy(() -> orderService.checkout(user.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");

        assertThat(userMapper.findById(user.getId()).getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    @Transactional
    void checkoutFailsWhenCartEmpty() {
        User user = newUser(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> orderService.checkout(user.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("购物车为空");
    }

    @Test
    void concurrentCheckoutDoesNotOversell() throws Exception {
        // 库存 1，两个读者同时结算，只能成功一单（此用例需要真实提交事务，不开启测试事务回滚，结束后清理数据）
        Book book = newBook("并发扣减测试书", "10.00", 1);
        User u1 = newUser(new BigDecimal("100.00"));
        User u2 = newUser(new BigDecimal("100.00"));
        cartItemMapper.upsert(u1.getId(), book.getId(), 1);
        cartItemMapper.upsert(u2.getId(), book.getId(), 1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (User u : List.of(u1, u2)) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    orderService.checkout(u.getId());
                    return true;
                } catch (BizException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        int success = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(15, TimeUnit.SECONDS)) {
                success++;
            }
        }
        pool.shutdown();

        assertThat(success).isEqualTo(1);
        assertThat(bookMapper.findById(book.getId()).getStock()).isZero();

        bookMapper.deleteById(book.getId());
        cleanup(u1, u2);
    }

    private void cleanup(User... users) {
        for (User u : users) {
            List<Order> orders = orderMapper.findByUserId(u.getId());
            if (!orders.isEmpty()) {
                orderItemMapper.deleteByOrderIds(orders.stream().map(Order::getId).toList());
                orderMapper.deleteByUserId(u.getId());
            }
            cartItemMapper.deleteByUserId(u.getId());
            userMapper.deleteById(u.getId());
        }
    }
}
