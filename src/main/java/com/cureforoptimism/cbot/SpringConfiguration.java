package com.cureforoptimism.cbot;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class SpringConfiguration {
  @Bean
  public CoinGeckoApiClient coinGeckoApiClient() {
    return new CoinGeckoApiClientImpl();
  }
}
