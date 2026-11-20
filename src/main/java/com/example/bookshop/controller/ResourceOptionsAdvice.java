package com.example.bookshop.controller;
import com.example.bookshop.dto.ResourceOptions;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@ControllerAdvice(assignableTypes={AdminBookController.class,ShopBookController.class,ResourceDetailController.class})
public class ResourceOptionsAdvice {
    @ModelAttribute("resourceTypes") public List<String> types(){ return ResourceOptions.TYPES; }
    @ModelAttribute("languages") public List<String> languages(){ return ResourceOptions.LANGUAGES; }
}
