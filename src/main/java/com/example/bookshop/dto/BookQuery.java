package com.example.bookshop.dto;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
public class BookQuery {
    private String keyword = "";
    private String field = "title";
    private String sort = "newest";
    private String stockStatus = "all";
    private Long categoryId;
    private List<Long> categoryIds = new ArrayList<>();
    private List<String> resourceTypes = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private int page = 1;
    private int size = 10;
    public int getSize() { return Set.of(10,20,50).contains(size) ? size : 10; }
    public void setSize(int v) { size = v; }
    private int threshold = 5;
    public void normalize(int threshold, boolean admin) {
        keyword = keyword == null ? "" : keyword.trim();
        if (field == null || !Set.of("title","author","publisher","identifier","all").contains(field)) field = "title";
        if (sort == null || !Set.of("newest", "priceAsc", "priceDesc").contains(sort)) sort = "newest";
        if (!admin || stockStatus == null || !Set.of("low", "empty").contains(stockStatus)) stockStatus = "all";
        if (categoryId != null && categoryId < 0) categoryId = null;
        categoryIds = categoryIds == null ? new ArrayList<>() : categoryIds.stream().filter(v -> v != null && v >= 0).distinct().limit(100).toList();
        resourceTypes = resourceTypes == null ? new ArrayList<>() : resourceTypes.stream().filter(v -> v != null && !v.isBlank()).distinct().limit(30).toList();
        languages = languages == null ? new ArrayList<>() : languages.stream().filter(v -> v != null && !v.isBlank()).distinct().limit(30).toList();
        page = Math.max(1, page);
        this.threshold = threshold;
    }
    public String getKeyword() { return keyword; }
    public void setKeyword(String v) { keyword = v; }
    public String getField() { return field; }
    public void setField(String v) { field = v; }
    public String getSort() { return sort; }
    public void setSort(String v) { sort = v; }
    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String v) { stockStatus = v; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long v) { categoryId = v; }
    public List<Long> getCategoryIds(){ return categoryIds; }
    public void setCategoryIds(List<Long> v){ categoryIds=v; }
    public List<String> getResourceTypes(){ return resourceTypes; }
    public void setResourceTypes(List<String> v){ resourceTypes=v; }
    public List<String> getLanguages(){ return languages; }
    public void setLanguages(List<String> v){ languages=v; }
    public int getPage() { return page; }
    public void setPage(int v) { page = v; }
    public int getThreshold() { return threshold; }
}

