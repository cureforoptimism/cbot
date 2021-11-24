package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.domain.Wallet;
import com.cureforoptimism.cbot.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository transactionRepository;
  private final UserService userService;
  private final CoinGeckoService coinGeckoService;

  public BigDecimal getUsdValue(Long userId, Long serverId) {
    // TODO: Use current token values, mapped to USD. Not just USD.
    final var transactions =
        transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
    BigDecimal usdValue = BigDecimal.ZERO;
    for (Transaction transaction : transactions) {
      if (transaction.getSymbol().equalsIgnoreCase("usd")) {
        usdValue = transaction.getAmount().add(usdValue);
      }
    }

    return usdValue;
  }

  public BigDecimal getToken(Long userId, Long serverId, String symbol) {
    return transactionRepository
        .findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId)
        .stream()
        .filter(tx -> tx.getSymbol().equalsIgnoreCase(symbol))
        .map(Transaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Set<Transaction> getAllTransactions(Long userId, Long serverId) {
    return transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(
        userId, serverId);
  }

  public Map<String, BigDecimal> getAllTokenAmounts(Long userId, Long serverId) {
    Set<Transaction> transactions =
        transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
    Wallet wallet = new Wallet(transactions, coinGeckoService);
    return wallet.getTokenAmounts();
  }

  public Map<String, BigDecimal> getAllTokenValues(Long userId, Long serverId) {
    Set<Transaction> transactions =
        transactionRepository.findByUser_DiscordIdEqualsAndUser_Server_DiscordId(userId, serverId);
    Wallet wallet = new Wallet(transactions, coinGeckoService);
    return wallet.getTokenValuesInUsd();
  }

  // TODO: Throw custom exceptions on invalid/failed sells
  @Transactional
  public Optional<Transaction> sell(Long userId, Long serverId, String symbol, BigDecimal amount) {
    Optional<User> user = userService.findByDiscordIdAndServerId(userId, serverId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    final var token = coinGeckoService.getCurrentPrice(symbol);
    final var tokensAvailable = getToken(userId, serverId, symbol);
    if (tokensAvailable.compareTo(amount) < 0) {
      return Optional.empty();
    }

    final var sellPrice = token.multiply(amount);

    // Deduct token
    transactionRepository.save(
        Transaction.builder()
            .user(user.get())
            .purchasePrice(token)
            .amount(amount.negate())
            .symbol(symbol)
            .build());

    // Add USD
    return Optional.of(
        transactionRepository.save(
            Transaction.builder()
                .user(user.get())
                .purchasePrice(BigDecimal.ONE)
                .amount(sellPrice)
                .symbol("usd")
                .build()));
  }

  // TODO: Throw custom exceptions on invalid/failed buys
  @Transactional
  public Optional<Transaction> buy(Long userId, Long serverId, String symbol, BigDecimal amount) {
    Optional<User> user = userService.findByDiscordIdAndServerId(userId, serverId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    final var token = coinGeckoService.getCurrentPrice(symbol);
    // TODO: Fees, etc
    BigDecimal usdInWallet = getUsdValue(userId, serverId);
    if ((token.multiply(amount)).compareTo(usdInWallet) <= 0) {
      BigDecimal purchasePrice = token.multiply(amount);

      // Deduct USD
      transactionRepository.save(
          Transaction.builder()
              .user(user.get())
              .purchasePrice(BigDecimal.ONE)
              .amount(purchasePrice.negate())
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
