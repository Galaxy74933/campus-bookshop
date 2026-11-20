package com.example.bookshop.entity;

import java.time.LocalDateTime;

public class ImportBatch {
    private Long id, ownerId;
    private String filename;
    private int resourceCount;
    private LocalDateTime createdAt, undoneAt;
    public Long getId() { return id; }
    public void setId(Long v) { id=v; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long v) { ownerId=v; }
    public String getFilename() { return filename; }
    public void setFilename(String v) { filename=v; }
    public int getResourceCount() { return resourceCount; }
    public void setResourceCount(int v) { resourceCount=v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt=v; }
    public LocalDateTime getUndoneAt() { return undoneAt; }
    public void setUndoneAt(LocalDateTime v) { undoneAt=v; }
}
