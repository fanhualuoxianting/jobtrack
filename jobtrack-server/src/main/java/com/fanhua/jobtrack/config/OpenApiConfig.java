package com.fanhua.jobtrack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobTrackOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("JobTrack API")
                .description("JobTrack 实习投递与面试管理平台接口文档")
                .version("1.0.0")
                .contact(new Contact().name("fanhua"))
            );
    }
}
