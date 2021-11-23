package com.cureforoptimism.cbot.repository;

import com.cureforoptimism.cbot.domain.Transaction;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  // There's no way this is efficient
  Set<Transaction> findByUser_DiscordIdEqualsAndUser_Server_DiscordId(
      Long discordId, Long serverDiscordId);
}
