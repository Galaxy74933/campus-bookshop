package com.example.bookshop.entity;
public class ReadingList {
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private boolean publicList;
    private int bookCount;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isPublicList() { return publicList; }
    public void setPublicList(boolean publicList) { this.publicList = publicList; }
    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }
}

