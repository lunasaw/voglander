package io.github.lunasaw.voglander.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 配置类
 * 配置 Swagger API 文档的基本信息
 *
 * @author luna
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Voglander API 文档")
                .description("Voglander视频监控设备管理平台 API 接口文档")
                .version("1.0.2")
                .contact(new Contact()
                    .name("luna")
                    .email("iszychen@gmail.com")
                    .url("https://github.com/lunasaw/voglander"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8081")
                    .description("开发环境"),
                new Server()
                    .url("https://your-production-domain.com")
                    .description("生产环境")));
    }
}