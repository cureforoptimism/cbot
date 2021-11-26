package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  Set<Transaction> findByUser_DiscordIdEqualsAndUser_Server_DiscordId(
      Long discordId, Long serverDiscordId);

  Set<Transaction>
      findByUser_DiscordIdAndUser_Server_DiscordIdAndSymbolIgnoreCaseAndTransactionType(
          Long discordId, Long discordId1, String symbol, TransactionType transactionType);
}
