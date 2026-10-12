package org.nutricoach.config;

import org.nutricoach.nutrition.application.NutritionCalculationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NutritionConfiguration {
    @Bean
    NutritionCalculationService nutritionCalculationService() {
        return new NutritionCalculationService();
    }
}
