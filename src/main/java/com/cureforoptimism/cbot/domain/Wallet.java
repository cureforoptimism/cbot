package com.cureforoptimism.cbot.domain;

import com.cureforoptimism.cbot.service.CoinGeckoService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Wallet {
  private final Map<String, BigDecimal> wallet;
  private final CoinGeckoService coinGeckoService;

  public Wallet(Set<Transaction> transactions, CoinGeckoService coinGeckoService) {
    wallet = new TreeMap<>();

    for (Transaction transaction : transactions) {
      if (!wallet.containsKey(transaction.getSymbol())) {
        wallet.put(transaction.getSymbol(), BigDecimal.ZERO);
      }

      BigDecimal currentValue = wallet.get(transaction.getSymbol());
      wallet.put(transaction.getSymbol(), currentValue.add(transaction.getAmount()));
    }

    this.coinGeckoService = coinGeckoService;
  }

  public Map<String, BigDecimal> getTokenAmounts() {
    return wallet;
  }

  public Map<String, BigDecimal> getTokenValuesInUsd() {
    Map<String, BigDecimal> values = new HashMap<>();

    for (Map.Entry<String, BigDecimal> entry : wallet.entrySet()) {
      if (entry.getKey().equalsIgnoreCase("usd")) {
        values.put("usd", entry.getValue());
      } else {
        values.put(
            entry.getKey(),
            coinGeckoService.getCurrentPrice(entry.getKey()).multiply(entry.getValue()));
      }
    }

    return values;
  }
}
