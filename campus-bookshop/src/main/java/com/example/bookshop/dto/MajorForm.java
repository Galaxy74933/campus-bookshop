package com.example.bookshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MajorForm {
    private Long id;
    @NotBlank(message = "专业名称不能为空")
    @Size(max = 100, message = "专业名称不能超过100个字符")
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
