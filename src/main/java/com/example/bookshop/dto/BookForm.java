package com.example.bookshop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class BookForm {
    private Long id;
    @NotNull(message="请选择主分类") private Long categoryId;
    @Size(max=20, message="附加分类最多20项") private List<Long> categoryIds = new ArrayList<>();
    @NotBlank(message="资源名称不能为空") @Size(max=200,message="名称不能超过200字符") private String title;
    @NotBlank(message="请选择资源类型") private String resourceType="图书";
    @Size(max=100,message="作者不能超过100字符") private String author;
    @Size(max=200,message="作者单位不能超过200字符") private String authorUnit;
    private String language="";
    @Size(max=200,message="来源/出版社不能超过200字符") private String publisher;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate publishDate;
    @Size(max=200,message="ISBN/DOI不能超过200字符") private String identifier;
    @Size(max=2000,message="简介不能超过2000字符") private String description;
    private BigDecimal price;
    private Integer stock;
    @AssertTrue(message="图书价格必填且大于0，最多两位小数和8位整数；库存必填且为非负整数")
    public boolean isSaleFieldsValid() {
        if (!"图书".equals(resourceType)) return true;
        return price!=null && price.signum()>0 && price.compareTo(new BigDecimal("99999999.99"))<=0
            && price.stripTrailingZeros().scale()<=2 && stock!=null && stock>=0;
    }
    public Long getId() { return id; }
    public void setId(Long v) { id=v; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long v) { categoryId=v; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> v) { categoryIds=v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title=v; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String v) { resourceType=v; }
    public String getAuthor() { return author; }
    public void setAuthor(String v) { author=v; }
    public String getAuthorUnit() { return authorUnit; }
    public void setAuthorUnit(String v) { authorUnit=v; }
    public String getLanguage() { return language; }
    public void setLanguage(String v) { language=v; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String v) { publisher=v; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate v) { publishDate=v; }
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String v) { identifier=v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description=v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { price=v; }
    public Integer getStock() { return stock; }
    public void setStock(Integer v) { stock=v; }
}
