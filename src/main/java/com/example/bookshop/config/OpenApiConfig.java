package com.example.bookshop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 配置：Swagger 文档的基础信息。
 * 本系统为服务端渲染（表单提交 + 重定向），非 RESTful JSON 接口，仅本地运行。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("网上购书系统 API")
                        .version("1.0.1")
                        .description("网上购书系统接口文档（服务端渲染页面 + 表单提交，未授权访问统一 302 跳转登录页）")
                        .contact(new Contact().name("Bookshop 开发团队"))
                        .license(new License().name("教学演示项目")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发环境")));
    }
}
