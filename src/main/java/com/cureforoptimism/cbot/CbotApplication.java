package com.cureforoptimism.cbot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
@AllArgsConstructor
@EnableJpaRepositories(basePackages = "com.cureforoptimism.cbot.repository")
public class CbotApplication {
  public static void main(String[] args) {
    SpringApplication.run(CbotApplication.class, args);
  }
}
