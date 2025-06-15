package com.example.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        Server apiGatewayServer = new Server()
                .url("http://localhost:8080/payment")
                .description("API Gateway Server");

        return new OpenAPI()
                .addServersItem(apiGatewayServer)
                .info(new Info()
                        .title("Payment Service API")
                        .description("API для управления платежами и счетами пользователей")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Payment Service Team")
                                .email("payment@example.com")));
    }
} 