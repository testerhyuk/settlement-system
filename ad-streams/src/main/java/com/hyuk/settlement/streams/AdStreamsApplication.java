package com.hyuk.settlement.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication(scanBasePackages = {"com.hyuk.settlement"})
@EnableJpaRepositories(basePackages = {"com.hyuk.settlement"})
@EntityScan(basePackages = {"com.hyuk.settlement"})
@EnableKafkaStreams
public class AdStreamsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdStreamsApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
