package com.warehouse.dispatch.config;

import com.warehouse.dispatch.service.DispatchPlannerEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public DispatchPlannerEngine dispatchPlannerEngine() {
        return new DispatchPlannerEngine();
    }
}
