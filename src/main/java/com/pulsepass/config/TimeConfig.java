package com.pulsepass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Reloj inyectable: en produccion es el real, en los tests se reemplaza por Clock.fixed(...). */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
