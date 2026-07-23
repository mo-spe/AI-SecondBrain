package com.secondbrain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Knife4j API文档配置. <p>配置 OpenAPI 元信息</p> */
@Configuration
public class Knife4jConfig {

    /**
     * 创建 OpenAPI 配置实例.
     *
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Contact contact = new Contact();
        contact.setName("SecondBrain Team");

        Info info = new Info();
        info.setTitle("AI-SecondBrain API文档");
        info.setVersion("1.0.0");
        info.setDescription("AI对话知识沉淀系统接口文档");
        info.setContact(contact);

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(info);
        return openAPI;
    }
}
