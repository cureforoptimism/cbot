package com.cureforoptimism.cbot.service;

import com.cureforoptimism.cbot.Constants;
import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.TransactionType;
import com.cureforoptimism.cbot.domain.User;
import com.cureforoptimism.cbot.domain.Wallet;
import com.cureforoptimism.cbot.domain.exceptions.InsufficientFundsException;
import com.cureforoptimism.cbot.domain.exceptions.TransactionException;
import com.cureforoptimism.cbot.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  // TODO: This is not accurate. It needs to account for X coins at Y price instead of number of transactions. Fix. Maybe write a unit test, because seriously.
  public BigDecimal getAverageBuyPrice(Long userId, Long serverId, String symbol) {
    if (symbol.equalsIgnoreCase("usd")) {
      return BigDecimal.ONE;
    }

    Set<Transaction> transactions =
        transactionRepository
            .findByUser_DiscordIdAndUser_Server_DiscordIdAndSymbolIgnoreCaseAndTransactionType(
                userId, serverId, symbol, TransactionType.BUY);

    if (transactions.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal sum =
        transactions.stream()
            .map(Transaction::getPurchasePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(new BigDecimal(transactions.size()), RoundingMode.FLOOR);
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

  @Transactional
  public Optional<Transaction> sell(Long userId, Long serverId, String symbol, BigDecimal amount)
      throws TransactionException {
    Optional<User> user = userService.findByDiscordIdAndServerId(userId, serverId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    final var token = coinGeckoService.getCurrentPrice(symbol);
    final var tokensAvailable = getToken(userId, serverId, symbol);
    if (tokensAvailable.compareTo(amount) < 0) {
      throw new InsufficientFundsException();
    }

    final var sellPrice = token.multiply(amount);
    final BigDecimal fees = token.multiply(amount).multiply(Constants.BUY_SELL_FEE_PCT);

    // Deduct token
    transactionRepository.save(
        Transaction.builder()
            .user(user.get())
            .purchasePrice(token)
            .amount(amount.negate())
            .symbol(symbol)
            .transactionType(TransactionType.SELL)
            .build());

    // Deduct fees
    transactionRepository.save(
        Transaction.builder()
            .user(user.get())
            .purchasePrice(token)
            .amount(fees.negate())
            .symbol(symbol)
            .transactionType(TransactionType.FEE)
            .build());

    // Add USD
    return Optional.of(
        transactionRepository.save(
            Transaction.builder()
                .user(user.get())
                .purchasePrice(BigDecimal.ONE)
                .amount(sellPrice)
                .fees(fees)
                .symbol("usd")
                .transactionType(TransactionType.BUY)
                .build()));
  }

  @Transactional
  public Optional<Transaction> buy(Long userId, Long serverId, String symbol, BigDecimal amount)
      throws TransactionException {
    Optional<User> user = userService.findByDiscordIdAndServerId(userId, serverId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    final var token = coinGeckoService.getCurrentPrice(symbol);
    BigDecimal fees = token.multiply(amount).multiply(Constants.BUY_SELL_FEE_PCT);

    BigDecimal usdInWallet = getUsdValue(userId, serverId);
    if ((token.multiply(amount).add(fees)).compareTo(usdInWallet) <= 0) {
      BigDecimal purchasePrice = token.multiply(amount);

      // Deduct USD
      transactionRepository.save(
          Transaction.builder()
              .user(user.get())
              .purchasePrice(BigDecimal.ONE)
              .amount(purchasePrice.negate())
              .symbol("usd")
              .transactionType(TransactionType.SELL)
              .build());

      // Deduct fees
      transactionRepository.save(
          Transaction.builder()
              .user(user.get())
              .purchasePrice(BigDecimal.ONE)
              .amount(fees.negate())
              .symbol("usd")
              .transactionType(TransactionType.FEE)
              .build());

      // Add coin
      return Optional.of(
          transactionRepository.save(
              Transaction.builder()
                  .user(user.get())
                  .purchasePrice(token)
                  .symbol(symbol.toLowerCase())
                  .amount(amount)
                  .fees(fees)
                  .transactionType(TransactionType.BUY)
                  .build()));
    } else {
      throw new InsufficientFundsException();
    }
  }
}
