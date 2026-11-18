package com.example.bookshop.service;

import com.example.bookshop.dto.CartItemView;
import com.example.bookshop.entity.Book;
import com.example.bookshop.entity.CartItem;
import com.example.bookshop.exception.BizException;
import com.example.bookshop.mapper.BookMapper;
import com.example.bookshop.mapper.CartItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final BookMapper bookMapper;

    public CartService(CartItemMapper cartItemMapper, BookMapper bookMapper) {
        this.cartItemMapper = cartItemMapper;
        this.bookMapper = bookMapper;
    }

    /**
     * 加入购物车：同一图书自动累加数量，累计数量不能超过当前库存。
     */
    @org.springframework.transaction.annotation.Transactional
    public void add(Long userId, Long bookId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BizException("购买数量至少为 1");
        }
        Book book = bookMapper.findByIdForUpdate(bookId);
        if (book == null) {
            throw new BizException("图书不存在或已下架");
        }
        if (!book.isPurchasable()) throw new BizException("该资源仅供浏览，不能购买");
        CartItem existing = cartItemMapper.findByUserIdAndBookId(userId, bookId);
        int current = existing == null ? 0 : existing.getQuantity();
        if (current + quantity > book.getStock()) {
            throw new BizException("《" + book.getTitle() + "》库存不足：当前库存 "
                    + book.getStock() + " 本，购物车已有 " + current + " 本");
        }
        cartItemMapper.upsert(userId, bookId, quantity);
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateQuantity(Long userId, Long itemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BizException("购买数量至少为 1");
        }
        CartItem item = cartItemMapper.findById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车记录不存在");
        }
        Book book = bookMapper.findByIdForUpdate(item.getBookId());
        if (book == null) {
            throw new BizException("图书不存在或已下架");
        }
        if (!book.isPurchasable()) throw new BizException("该资源仅供浏览，不能购买");
        if (quantity > book.getStock()) {
            throw new BizException("《" + book.getTitle() + "》库存不足：当前库存 "
                    + book.getStock() + " 本");
        }
        cartItemMapper.updateQuantity(itemId, quantity);
    }

    public void remove(Long userId, Long itemId) {
        CartItem item = cartItemMapper.findById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车记录不存在");
        }
        cartItemMapper.deleteById(itemId);
    }

    public void clear(Long userId) {
        cartItemMapper.deleteByUserId(userId);
    }

    public List<CartItemView> cartView(Long userId) {
        return cartItemMapper.selectViewByUserId(userId);
    }
}
