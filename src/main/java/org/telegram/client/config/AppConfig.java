package org.telegram.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableAsync;
import org.telegram.client.controllers.RestController;
import org.telegram.client.service.TgClientService;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean
    @Scope("singleton")
    public RestController restController(TgClientService tgClientService) {
        return new RestController(tgClientService);
    }
}
