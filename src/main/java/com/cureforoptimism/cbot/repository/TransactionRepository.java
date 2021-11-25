package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.Transaction;
import com.cureforoptimism.cbot.domain.TransactionType;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  // There's no way this is efficient
  Set<Transaction> findByUser_DiscordIdEqualsAndUser_Server_DiscordId(
      Long discordId, Long serverDiscordId);

  Set<Transaction> findByUser_DiscordIdAndSymbolIgnoreCaseAndUser_Server_DiscordId(
      Long discordId, String symbol, Long discordId1);

  Set<Transaction>
      findByUser_DiscordIdAndUser_Server_DiscordIdAndSymbolIgnoreCaseAndTransactionType(
          Long discordId, Long discordId1, String symbol, TransactionType transactionType);
}
