package com.example.bookshop.service;

import com.example.bookshop.dto.OrderView;
import com.example.bookshop.entity.Book;
import com.example.bookshop.entity.CartItem;
import com.example.bookshop.entity.Order;
import com.example.bookshop.entity.OrderItem;
import com.example.bookshop.entity.User;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.BookMapper;
import com.example.bookshop.mapper.CartItemMapper;
import com.example.bookshop.mapper.OrderItemMapper;
import com.example.bookshop.mapper.OrderMapper;
import com.example.bookshop.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    public OrderService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                        CartItemMapper cartItemMapper, BookMapper bookMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartItemMapper = cartItemMapper;
        this.bookMapper = bookMapper;
        this.userMapper = userMapper;
    }

    /**
     * 结算支付：清空购物车并用购书款支付。全程一个事务，任一校验失败整体回滚：
     * 1. 逐项条件扣减库存（stock >= quantity 才更新），防止超卖；
     * 2. 按数据库价格计算总额，条件扣减购书款（balance >= total 才更新），防止透支；
     * 3. 生成订单和明细，清空购物车。
     */
    @Transactional(rollbackFor = Exception.class)
    public Order checkout(Long userId) {
        List<CartItem> items = cartItemMapper.findByUserId(userId);
        if (items.isEmpty()) {
            throw new BizException("购物车为空，请先挑选图书");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : items) {
            Book book = bookMapper.findById(item.getBookId());
            if (book == null) {
                throw new BizException("购物车中的图书不存在或已下架");
            }
            if (book.getStock() < item.getQuantity()) {
                throw new BizException("《" + book.getTitle() + "》库存不足：当前库存 "
                        + book.getStock() + " 本，需要 " + item.getQuantity() + " 本");
            }
            if (bookMapper.deductStock(book.getId(), item.getQuantity()) == 0) {
                throw new BizException("《" + book.getTitle() + "》库存不足，请稍后重试");
            }
            BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);

            OrderItem oi = new OrderItem();
            oi.setBookId(book.getId());
            oi.setBookTitle(book.getTitle());
            oi.setBookPrice(book.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setSubtotal(subtotal);
            orderItems.add(oi);
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (user.getBalance().compareTo(total) < 0) {
            throw new BizException("购书款不足：订单总额 " + total.toPlainString()
                    + " 元，当前余额 " + user.getBalance().toPlainString() + " 元");
        }
        if (userMapper.deductBalance(userId, total) == 0) {
            throw new BizException("购书款不足，请稍后重试");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(total);
        orderMapper.insert(order);

        orderItems.forEach(oi -> oi.setOrderId(order.getId()));
        orderItemMapper.insertBatch(orderItems);

        cartItemMapper.deleteByUserId(userId);
        return order;
    }

    public List<OrderView> listByUser(Long userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> ids = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> byOrderId = orderItemMapper.selectByOrderIds(ids).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
                .map(o -> new OrderView(o, byOrderId.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    private String generateOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(100, 1000);
        return "BS" + ts + rand;
    }
}
