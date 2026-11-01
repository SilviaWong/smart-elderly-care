package com.example.elderlycare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI elderlyCareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧养老管理系统 API 文档")
                        .description("ToB 智慧养老机构全业务运营管理平台接口，涵盖系统管理、老人档案、空间床位、工单、健康、护理、餐饮、财务与风险处置。")
                        .version("1.0.0")
                        .contact(new Contact().name("技术研发团队").email("dev@elderlycare.example.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入形如 'Bearer {token}' 的 JWT 认证令牌")));
    }
}
