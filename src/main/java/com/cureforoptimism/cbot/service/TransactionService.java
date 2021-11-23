package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.domain.Wallet;
import com.cureforoptimism.cbot.repository.TransactionRepository;
import java.util.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository transactionRepository;
  private final UserService userService;
  private final CoinGeckoService coinGeckoService;

  public Double getUsdValue(Long userId, Long serverId) {
    // TODO: Use current token values, mapped to USD. Not just USD.
    final var transactions =
        transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
    Double usdValue = 0.0d;
    for (Transaction transaction : transactions) {
      if (transaction.getSymbol().equalsIgnoreCase("usd")) {
        usdValue += transaction.getAmount();
      }
    }

    return usdValue;
  }

  public Set<Transaction> getAllTransactions(Long userId, Long serverId) {
    return transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(
        userId, serverId);
  }

  public Map<String, Double> getAllTokenAmounts(Long userId, Long serverId) {
    Set<Transaction> transactions =
        transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
    Wallet wallet = new Wallet(transactions, coinGeckoService);
    return wallet.getTokenAmounts();
  }

  public Map<String, Double> getAllTokenValues(Long userId, Long serverId) {
    Set<Transaction> transactions =
        transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
    Wallet wallet = new Wallet(transactions, coinGeckoService);
    return wallet.getTokenValuesInUsd();
  }

  // TODO: Throw custom exceptions on invalid/failed buys instead of just boolean
  @Transactional
  public Optional<Transaction> buy(Long userId, Long serverId, String symbol, Double amount) {
    Optional<User> user = userService.findByDiscordIdAndServerId(userId, serverId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    final var token = coinGeckoService.getCurrentPrice(symbol);
    // TODO: Fees, etc
    Double usdInWallet = getUsdValue(userId, serverId);
    if ((token.longValue() * amount) <= usdInWallet) {
      Double purchasePrice = token * amount;

      // Deduct USD
      transactionRepository.save(
          Transaction.builder()
              .user(user.get())
              .purchasePrice(1.0d)
              .amount(-purchasePrice)
              .symbol("usd")
              .build());

      // Add coin
      return Optional.of(
          transactionRepository.save(
              Transaction.builder()
                  .user(user.get())
                  .purchasePrice(token)
                  .symbol(symbol.toLowerCase())
                  .amount(amount)
                  .build()));
    }

    // This is ambiguous. Custom exceptions will be better.
    return Optional.empty();
  }
}
