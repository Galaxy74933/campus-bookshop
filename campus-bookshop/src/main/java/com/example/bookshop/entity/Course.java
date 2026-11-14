package com.example.bookshop.entity;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private Long id;
    private String name;
    private List<Major> majors = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Major> getMajors() { return majors; }
    public void setMajors(List<Major> majors) { this.majors = majors; }
}
