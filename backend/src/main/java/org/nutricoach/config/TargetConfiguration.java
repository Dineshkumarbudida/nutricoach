package org.nutricoach.config;

import org.nutricoach.targets.application.DailyTargetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TargetConfiguration {
    @Bean
    DailyTargetService dailyTargetService() {
        return new DailyTargetService();
    }
}
