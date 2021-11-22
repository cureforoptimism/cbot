package com.cureforoptimism.cbot.services;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.Coins.CoinList;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class CoinGeckoService {
  private final CoinGeckoApiClient client;
  private Map<String, String> coinTickerToIdMap;

  public CoinGeckoService(CoinGeckoApiClient coinGeckoApiClient) {
    this.client = coinGeckoApiClient;

    refreshTokenList();
  }

  public Double getCurrentPrice(String symbol) {
    return client
        .getPrice(coinTickerToIdMap.get(symbol), Currency.USD)
        .get(coinTickerToIdMap.get(symbol))
        .get("usd");
  }

  @Scheduled(fixedDelay = 60000)
  public void refreshTokenList() {
    List<CoinList> coinList = client.getCoinList();

    coinTickerToIdMap =
            coinList.stream()
                    .collect(
                            Collectors.toMap(
                                    CoinList::getSymbol, CoinList::getId, (existing, replacement) -> existing));
  }
}
