package com.example.bookshop.config;
import com.example.bookshop.service.CategoryService;
import com.example.bookshop.service.DemoResourceService;
import com.example.bookshop.service.DemoReadingListService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
@Component
public class CatalogInitializer implements ApplicationRunner {
    private final CategoryService categories;
    private final DemoResourceService demo;
    private final DemoReadingListService readingDemo;
    public CatalogInitializer(CategoryService categories,DemoResourceService demo,DemoReadingListService readingDemo) { this.categories=categories; this.demo=demo; this.readingDemo=readingDemo; }
    @Override public void run(ApplicationArguments args) throws Exception { categories.initializeDefaults(); demo.initialize(); readingDemo.initialize(); }
}

