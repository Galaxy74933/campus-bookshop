package com.example.bookshop.dto;

import com.example.bookshop.entity.Order;
import com.example.bookshop.entity.OrderItem;

import java.util.List;

/**
 * 订单展示对象（订单 + 明细列表）。
 */
public class OrderView {

    private final Order order;
    private final List<OrderItem> items;

    public OrderView(Order order, List<OrderItem> items) {
        this.order = order;
        this.items = items;
    }

    public Order getOrder() {
        return order;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
