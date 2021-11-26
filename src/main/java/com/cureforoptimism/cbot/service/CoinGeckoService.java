package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.SymbolToTickerCache;
import com.cureforoptimism.cbot.repository.SymbolToTickerCacheRepository;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import com.litesoftwares.coingecko.domain.Coins.CoinList;
import com.litesoftwares.coingecko.domain.Shared.Ticker;
import com.litesoftwares.coingecko.exception.CoinGeckoApiException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
@EnableScheduling
public class CoinGeckoService {
  private final CoinGeckoApiClient client;
  private final SymbolToTickerCacheRepository symbolToTickerCacheRepository;
  private Map<String, String> coinTickerToIdMap;
  private final Map<String, List<String>> collisions; // lazy; resolve collisions only on lookup

  // Cache coingecko values for a bit, just so we don't piss the API off. Note that this would need
  // to be a shared cache (e.g. redis) if you plan to run multiple instances.
  private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public CoinGeckoService(
      CoinGeckoApiClient client, SymbolToTickerCacheRepository symbolToTickerCacheRepository) {
    this.client = client;
    this.symbolToTickerCacheRepository = symbolToTickerCacheRepository;
    this.collisions = new ConcurrentHashMap<>();
  }

  @AllArgsConstructor
  private static class CacheEntry {
    Date persisted;
    BigDecimal value;
  }

  @Retryable(value = CoinGeckoApiException.class)
  public CoinFullData getFullCoinData(String symbol) {
    resolveCollisions(symbol);

    return client.getCoinById(coinTickerToIdMap.get(symbol));
  }

  @Retryable(value = CoinGeckoApiException.class)
  public BigDecimal getCurrentPrice(String symbol) {
    symbol = symbol.toLowerCase();

    if (cache.containsKey(symbol)) {
      if (!(System.currentTimeMillis() - cache.get(symbol).persisted.getTime()
          >= Constants.COIN_GECKO_CACHE_EXPIRY)) {
        return cache.get(symbol).value;
      }
    }

    resolveCollisions(symbol);

    BigDecimal value =
        BigDecimal.valueOf(
            client
                .getPrice(coinTickerToIdMap.get(symbol), Currency.USD)
                .get(coinTickerToIdMap.get(symbol))
                .get("usd"));

    cache.put(symbol, new CacheEntry(new Date(), value));

    return value;
  }

  @Retryable(value = CoinGeckoApiException.class)
  private void resolveCollisions(String symbol) {
    // Need to resolve the collision; use ticker with highest market cap
    if (collisions.containsKey(symbol)) {
      final var resolved = symbolToTickerCacheRepository.findById(symbol);
      if (resolved.isEmpty()) {
        Ticker highTicker = null;
        int highRank = Integer.MIN_VALUE;

        for (String collision : collisions.get(symbol)) {
          CoinFullData coinFullData = client.getCoinById(collision);

          if (coinFullData.getCoingeckoRank() < highRank && !coinFullData.getTickers().isEmpty()) {
            highTicker = coinFullData.getTickers().get(0); // Use first ticker
          }
        }

        if (highTicker != null) {
          symbolToTickerCacheRepository.save(
              new SymbolToTickerCache(symbol, highTicker.getCoinId()));
          coinTickerToIdMap.put(symbol, highTicker.getCoinId());
        }

        collisions.remove(symbol);
      } else {
        coinTickerToIdMap.put(symbol, resolved.get().getTicker());
      }
    }
  }

  // We really don't need to refresh this often
  @Scheduled(fixedDelay = 60000 * 60)
  public void refreshTokenList() {
    List<CoinList> coinList = client.getCoinList();

    // Hack: We need the key to build our collision map, so we can use this in the key mapping part
    // of toMap
    AtomicReference<String> key = new AtomicReference<>();
    coinTickerToIdMap =
        coinList.stream()
            .collect(
                Collectors.toMap(
                    item -> {
                      key.set(item.getSymbol());
                      return item.getSymbol();
                    },
                    CoinList::getId,
                    (existing, replacement) -> {
                      List<String> existingCollisions = collisions.get(key.get());
                      if (existingCollisions == null) {
                        existingCollisions = new ArrayList<>();
                        existingCollisions.add(existing);
                      }

                      existingCollisions.add(replacement);
                      collisions.put(key.get(), existingCollisions);

                      return existing;
                    }));
  }
}
