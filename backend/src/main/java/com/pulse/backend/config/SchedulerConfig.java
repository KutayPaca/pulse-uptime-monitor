package com.pulse.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Aynı anda paralel çalışabilecek ping görev iş parçacığı sayısı
        scheduler.setThreadNamePrefix("dynamic-ping-");
        scheduler.initialize();
        return scheduler;
    }
}