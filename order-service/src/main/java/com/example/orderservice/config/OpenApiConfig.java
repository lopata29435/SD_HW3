package com.example.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI orderServiceOpenAPI() {
        Server apiGatewayServer = new Server()
                .url("http://localhost:8080/order")
                .description("API Gateway Server");

        return new OpenAPI()
                .addServersItem(apiGatewayServer)
                .info(new Info()
                        .title("Order Service API")
                        .description("API для управления заказами пользователей")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Order Service Team")
                                .email("orders@example.com")));
    }
} 