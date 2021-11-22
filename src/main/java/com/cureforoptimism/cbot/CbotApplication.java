package com.cureforoptimism.cbot;

import com.cureforoptimism.cbot.services.CoinGeckoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
@AllArgsConstructor
public class CbotApplication {
  private final CoinGeckoService coinGeckoService;

  public static void main(String[] args) {
    SpringApplication.run(CbotApplication.class, args);
  }
}
