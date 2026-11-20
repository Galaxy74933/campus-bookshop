package com.example.bookshop.dto;
import java.util.List;
public final class ResourceOptions {
    private ResourceOptions() {}
    public static final List<String> TYPES = List.of("图书", "论文", "期刊", "会议论文", "学位论文", "报告", "标准", "其他");
    public static final List<String> LANGUAGES = List.of("中文", "外文", "中外文对照", "其他");
}
