package com.example.bookshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class CourseForm {
    private Long id;
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称不能超过100个字符")
    private String name;
    private List<Long> majorIds = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Long> getMajorIds() { return majorIds; }
    public void setMajorIds(List<Long> majorIds) { this.majorIds = majorIds; }
}
