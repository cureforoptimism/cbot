package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.Constants;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.Coins.CoinList;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class CoinGeckoService {
  private final CoinGeckoApiClient client;
  private Map<String, String> coinTickerToIdMap;

  // Cache coingecko values for a bit, just so we don't piss the API off. Note that this would need
  // to be a shared cache (e.g. redis) if you plan to run multiple instances.
  private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public CoinGeckoService(CoinGeckoApiClient client, Map<String, String> coinTickerToIdMap) {
    this.client = client;
    this.coinTickerToIdMap = coinTickerToIdMap;
  }

  @AllArgsConstructor
  private static class CacheEntry {
    Date persisted;
    BigDecimal value;
  }

  public BigDecimal getCurrentPrice(String symbol) {
    if (cache.containsKey(symbol)) {
      if (!(System.currentTimeMillis() - cache.get(symbol).persisted.getTime()
          >= Constants.COIN_GECKO_CACHE_EXPIRY)) {
        return cache.get(symbol).value;
      }
    }

    BigDecimal value =
        BigDecimal.valueOf(
            client
                .getPrice(coinTickerToIdMap.get(symbol), Currency.USD)
                .get(coinTickerToIdMap.get(symbol))
                .get("usd"));

    cache.put(symbol, new CacheEntry(new Date(), value));

    return value;
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
