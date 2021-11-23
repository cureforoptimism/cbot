package com.cureforoptimism.cbot.domain;

import com.cureforoptimism.cbot.service.CoinGeckoService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Wallet {
  private final Map<String, Double> wallet;
  private final CoinGeckoService coinGeckoService;

  public Wallet(Set<Transaction> transactions, CoinGeckoService coinGeckoService) {
    wallet = new HashMap<>();

    for (Transaction transaction : transactions) {
      if (!wallet.containsKey(transaction.getSymbol())) {
        wallet.put(transaction.getSymbol(), 0.0d);
      }

      Double currentValue = wallet.get(transaction.getSymbol());
      wallet.put(transaction.getSymbol(), currentValue + transaction.getAmount());
    }

    this.coinGeckoService = coinGeckoService;
  }

  public Map<String, Double> getTokenAmounts() {
    return wallet;
  }

  public Map<String, Double> getTokenValuesInUsd() {
    Map<String, Double> values = new HashMap<>();

    for (Map.Entry<String, Double> entry : wallet.entrySet()) {
      if (entry.getKey().equalsIgnoreCase("usd")) {
        values.put("usd", entry.getValue());
      } else {
        values.put(
            entry.getKey(), coinGeckoService.getCurrentPrice(entry.getKey()) * entry.getValue());
      }
    }

    return values;
  }
}
