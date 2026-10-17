package com.sagares.saga_res_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReservationTimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
