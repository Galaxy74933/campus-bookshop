package com.example.bookshop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 图书信息。
 */
public class Book {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private List<Long> categoryIds = new ArrayList<>();
    private String resourceType;
    private String language;
    private String publisher;
    private java.time.LocalDate publishDate;
    private String identifier;
    private String description;
    public List<Long> getCategoryIds(){ return categoryIds; }
    public void setCategoryIds(List<Long> v){ categoryIds = v == null ? new ArrayList<>() : v; }
    public String getResourceType(){ return resourceType == null ? "图书" : resourceType; }
    public boolean isPurchasable(){ return "图书".equals(getResourceType()); }
    private String authorUnit;
    public String getAuthorUnit(){ return authorUnit; }
    public void setAuthorUnit(String v){ authorUnit=v; }
    public void setResourceType(String v){ resourceType=v; }
    public String getLanguage(){ return language; }
    public void setLanguage(String v){ language=v; }
    public String getPublisher(){ return publisher; }
    public void setPublisher(String v){ publisher=v; }
    public java.time.LocalDate getPublishDate(){ return publishDate; }
    public void setPublishDate(java.time.LocalDate v){ publishDate=v; }
    public String getIdentifier(){ return identifier; }
    public void setIdentifier(String v){ identifier=v; }
    public String getDescription(){ return description; }
    public void setDescription(String v){ description=v; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long id) { categoryId = id; }
    public String getCategoryName() { return categoryName == null ? "待分类" : categoryName; }
    public void setCategoryName(String name) { categoryName = name; }
    private String title;
    private String author;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
